package co.edu.itm.prestamosdeportivos.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.DetallePrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.DevolucionRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoEspecificaciones;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoFiltros;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.DetallePrestamoRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.PrestamoResumenRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.PrestamoRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaDevolucion;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaImplemento;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaIncidencia;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;

/** Fachada de solo lectura para CU-15 y la consulta histórica de BE-11. */
@Service
public class PrestamoConsultaServicio {

    private static final Set<String> CAMPOS_ORDEN = Set.of("fechaPrestamo", "fechaLimiteDevolucion", "codigo", "estado");
    private static final Set<EstadoPrestamo> ESTADOS = EnumSet.allOf(EstadoPrestamo.class);

    private final PrestamoRepositorio prestamos;
    private final DetallePrestamoRepositorio detalles;
    private final DevolucionRepositorio devoluciones;
    private final IncidenciaRepositorio incidencias;
    private final ConversorFechas fechas;
    private final Clock reloj;

    public PrestamoConsultaServicio(PrestamoRepositorio prestamos, DetallePrestamoRepositorio detalles,
            DevolucionRepositorio devoluciones, IncidenciaRepositorio incidencias,
            ConversorFechas fechas, Clock reloj) {
        this.prestamos = prestamos;
        this.detalles = detalles;
        this.devoluciones = devoluciones;
        this.incidencias = incidencias;
        this.fechas = fechas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PaginaRespuesta<PrestamoResumenRespuesta> listar(int pagina, int tamano, String orden, String estado,
            UUID usuarioId, String identificacion, OffsetDateTime desde, OffsetDateTime hasta, String texto) {
        validarPaginacion(pagina, tamano);
        EstadoPrestamo estadoFiltro = parsearEstado(estado);
        Instant ahora = reloj.instant();
        Instant desdeInstant = desde == null ? null : desde.toInstant();
        Instant hastaInstant = hasta == null ? null : hasta.toInstant();
        if (desdeInstant != null && hastaInstant != null && !desdeInstant.isBefore(hastaInstant)) {
            throw invalidacion("El intervalo de fechas debe ser [desde, hasta).", "desde");
        }
        var filtros = new PrestamoFiltros(estadoFiltro, usuarioId, identificacion, desdeInstant, hastaInstant, texto, ahora);
        var paginaResultado = prestamos.findAll(PrestamoEspecificaciones.porFiltros(filtros), pageable(pagina, tamano, orden));
        List<UUID> ids = paginaResultado.getContent().stream().map(Prestamo::getId).toList();
        Map<UUID, Conteo> cantidades = ids.isEmpty() ? Map.of() : detalles.contarPorPrestamos(ids).stream()
                .collect(Collectors.toMap(DetallePrestamoRepositorio.ConteoPorPrestamo::getPrestamoId,
                        conteo -> new Conteo(conteo.getCantidad(), conteo.getPendientes())));
        return PaginaRespuesta.de(paginaResultado,
                prestamo -> resumen(prestamo, cantidades.getOrDefault(prestamo.getId(), new Conteo(0, 0)), ahora));
    }

    @Transactional(readOnly = true)
    public PrestamoRespuesta consultar(UUID id) {
        Prestamo prestamo = prestamos.findById(id)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO));
        Instant ahora = reloj.instant();
        EstadoPrestamo estado = estadoVisible(prestamo, ahora);
        List<DetallePrestamoRespuesta> detallesRespuesta = prestamo.getDetalles().stream()
                .map(detalle -> new DetallePrestamoRespuesta(detalle.getId(),
                        new ReferenciaImplemento(detalle.getImplemento().getId(), detalle.getImplemento().getCodigoInventario(),
                                detalle.getImplemento().getNombre(), detalle.getImplemento().getEstado()),
                        detalle.getEstado()))
                .toList();
        List<ReferenciaDevolucion> devolucionesRespuesta = devoluciones.findByPrestamoIdOrderByFechaDevolucionDesc(id).stream()
                .map(devolucion -> new ReferenciaDevolucion(devolucion.getId(), devolucion.getCodigo(),
                        fechas.aRespuesta(devolucion.getFechaDevolucion())))
                .toList();
        List<ReferenciaIncidencia> incidenciasRespuesta = incidencias.findByPrestamoIdOrderByFechaRegistroDesc(id).stream()
                .map(incidencia -> new ReferenciaIncidencia(incidencia.getId(), incidencia.getCodigo(), incidencia.getTipo(),
                        incidencia.getImplemento().getCodigoInventario()))
                .toList();
        int pendientes = (int) prestamo.getDetalles().stream()
                .filter(detalle -> detalle.getEstado() == EstadoDetallePrestamo.PENDIENTE_DEVOLUCION).count();
        return new PrestamoRespuesta(prestamo.getId(), prestamo.getCodigo(), estado,
                usuario(prestamo.getUsuario(), true), usuario(prestamo.getPrestador(), false),
                fechas.aRespuesta(prestamo.getFechaPrestamo()), fechas.aRespuesta(prestamo.getFechaLimiteDevolucion()),
                fechas.aRespuesta(prestamo.getFechaFinalizacion()), estado == EstadoPrestamo.VENCIDO,
                pendientes, detallesRespuesta, devolucionesRespuesta, incidenciasRespuesta);
    }

    private PrestamoResumenRespuesta resumen(Prestamo prestamo, Conteo conteo, Instant ahora) {
        EstadoPrestamo estado = estadoVisible(prestamo, conteo.pendientes() > 0, ahora);
        return new PrestamoResumenRespuesta(prestamo.getId(), prestamo.getCodigo(), estado,
                usuario(prestamo.getUsuario(), true), fechas.aRespuesta(prestamo.getFechaPrestamo()),
                fechas.aRespuesta(prestamo.getFechaLimiteDevolucion()), estado == EstadoPrestamo.VENCIDO,
                (int) conteo.cantidad());
    }

    private EstadoPrestamo estadoVisible(Prestamo prestamo, Instant ahora) {
        boolean pendiente = prestamo.getDetalles().stream()
                .anyMatch(detalle -> detalle.getEstado() == EstadoDetallePrestamo.PENDIENTE_DEVOLUCION);
        return estadoVisible(prestamo, pendiente, ahora);
    }

    private EstadoPrestamo estadoVisible(Prestamo prestamo, boolean pendiente, Instant ahora) {
        if (prestamo.getEstado() != EstadoPrestamo.FINALIZADO && pendiente
                && prestamo.getFechaLimiteDevolucion().isBefore(ahora)) {
            return EstadoPrestamo.VENCIDO;
        }
        return prestamo.getEstado();
    }

    private Sort pageableSort(String orden) {
        if (orden == null || orden.isBlank()) {
            return Sort.by(Sort.Order.desc("fechaPrestamo"), Sort.Order.asc("id"));
        }
        String[] partes = orden.split(",", -1);
        if (partes.length != 2 || !CAMPOS_ORDEN.contains(partes[0])
                || !(partes[1].equals("asc") || partes[1].equals("desc"))) {
            throw invalidacion("El orden debe usar un campo permitido y asc o desc.", "orden");
        }
        Sort.Direction direccion = Sort.Direction.fromString(partes[1]);
        return Sort.by(new Sort.Order(direccion, partes[0]), new Sort.Order(Sort.Direction.ASC, "id"));
    }

    private org.springframework.data.domain.Pageable pageable(int pagina, int tamano, String orden) {
        return PageRequest.of(pagina, tamano, pageableSort(orden));
    }

    private EstadoPrestamo parsearEstado(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            EstadoPrestamo estado = EstadoPrestamo.valueOf(valor.trim().toUpperCase(Locale.ROOT));
            if (!ESTADOS.contains(estado)) {
                throw new IllegalArgumentException();
            }
            return estado;
        } catch (IllegalArgumentException ex) {
            throw invalidacion("El estado del préstamo no es válido.", "estado");
        }
    }

    private void validarPaginacion(int pagina, int tamano) {
        if (pagina < 0) {
            throw invalidacion("La página debe ser mayor o igual a 0.", "pagina");
        }
        if (tamano < 1 || tamano > 100) {
            throw invalidacion("El tamaño debe estar entre 1 y 100.", "tamano");
        }
    }

    private ExcepcionDominio invalidacion(String detalle, String campo) {
        return new ExcepcionDominio(CodigoError.VALIDACION_FALLIDA, detalle,
                List.of(new ErrorCampo(campo, detalle)));
    }

    private static ReferenciaUsuario usuario(co.edu.itm.prestamosdeportivos.dominio.Usuario usuario,
            boolean identificacion) {
        return new ReferenciaUsuario(usuario.getId(), usuario.getNombres(), usuario.getApellidos(),
                identificacion ? usuario.getIdentificacion() : null);
    }

    private record Conteo(long cantidad, long pendientes) {
    }
}
