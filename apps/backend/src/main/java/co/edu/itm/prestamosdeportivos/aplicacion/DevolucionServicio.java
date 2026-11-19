package co.edu.itm.prestamosdeportivos.aplicacion;

import static co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.DevolucionRespuesta;
import static co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.DevolucionResumenRespuesta;
import static co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.IncidenciaEnDevolucionSolicitud;
import static co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.ItemDevolucionRespuesta;
import static co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.ItemDevolucionSolicitud;
import static co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.RegistrarDevolucionSolicitud;
import static co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.SancionAplicadaRespuesta;
import static co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaImplemento;
import static co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaIncidencia;
import static co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaPrestamo;
import static co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.itm.prestamosdeportivos.dominio.AlcanceDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.DetalleDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.DetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.Devolucion;
import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EventoEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.HistorialEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Incidencia;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Sancion;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.DevolucionEspecificaciones;
import co.edu.itm.prestamosdeportivos.persistencia.DevolucionFiltros;
import co.edu.itm.prestamosdeportivos.persistencia.DevolucionRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.HistorialEstadoImplementoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.SancionRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.aplicacion.sanciones.ContextoSancion;
import co.edu.itm.prestamosdeportivos.aplicacion.sanciones.PoliticaSancion;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fachada transaccional de CU-16 y CU-17: devolución, incidencia y cierre. */
@Service
public class DevolucionServicio {

    private static final Set<String> CAMPOS_ORDEN = Set.of("fechaDevolucion", "codigo", "alcance");

    private final DevolucionRepositorio devoluciones;
    private final PrestamoRepositorio prestamos;
    private final ImplementoDeportivoRepositorio implementos;
    private final IncidenciaRepositorio incidencias;
    private final SancionRepositorio sanciones;
    private final HistorialEstadoImplementoRepositorio historial;
    private final UsuarioRepositorio usuarios;
    private final ConversorFechas fechas;
    private final Clock reloj;
    private final List<PoliticaSancion> politicasSancion;

    public DevolucionServicio(DevolucionRepositorio devoluciones, PrestamoRepositorio prestamos,
            ImplementoDeportivoRepositorio implementos, IncidenciaRepositorio incidencias,
            SancionRepositorio sanciones, HistorialEstadoImplementoRepositorio historial,
            UsuarioRepositorio usuarios, ConversorFechas fechas, Clock reloj,
            List<PoliticaSancion> politicasSancion) {
        this.devoluciones = devoluciones;
        this.prestamos = prestamos;
        this.implementos = implementos;
        this.incidencias = incidencias;
        this.sanciones = sanciones;
        this.historial = historial;
        this.usuarios = usuarios;
        this.fechas = fechas;
        this.reloj = reloj;
        this.politicasSancion = List.copyOf(politicasSancion);
    }

    @Transactional
    public DevolucionRespuesta registrar(UUID prestamoId, RegistrarDevolucionSolicitud solicitud,
            UsuarioAutenticado operador) {
        exigirPrestador(operador);
        validarIdsNoRepetidos(solicitud.items());
        if (Boolean.TRUE.equals(solicitud.retrasoJustificado())
                && (solicitud.justificacion() == null || solicitud.justificacion().isBlank())) {
            throw new ExcepcionDominio(CodigoError.JUSTIFICACION_REQUERIDA,
                    "Debe registrar la justificación del retraso.",
                    List.of(new ErrorCampo("justificacion", "es obligatoria cuando retrasoJustificado es true")));
        }

        Prestamo prestamo = prestamos.bloquearPorId(prestamoId)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO,
                        "El préstamo solicitado no existe."));
        if (prestamo.getEstado() == EstadoPrestamo.FINALIZADO) {
            throw new ExcepcionDominio(CodigoError.PRESTAMO_FINALIZADO);
        }

        Map<UUID, DetallePrestamo> detallesPorImplemento = prestamo.getDetalles().stream()
                .collect(Collectors.toMap(detalle -> detalle.getImplemento().getId(), Function.identity()));
        Map<UUID, DetallePrestamo> seleccionados = new HashMap<>();
        for (int i = 0; i < solicitud.items().size(); i++) {
            ItemDevolucionSolicitud item = solicitud.items().get(i);
            DetallePrestamo detalle = detallesPorImplemento.get(item.implementoId());
            if (detalle == null) {
                throw new ExcepcionDominio(CodigoError.IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO,
                        "El implemento no pertenece al préstamo indicado.",
                        List.of(new ErrorCampo("items[" + i + "].implementoId", "no pertenece al préstamo")));
            }
            if (detalle.getEstado() != EstadoDetallePrestamo.PENDIENTE_DEVOLUCION) {
                throw new ExcepcionDominio(CodigoError.UNIDAD_YA_RESUELTA,
                        "La unidad " + detalle.getImplemento().getCodigoInventario()
                                + " ya fue devuelta o resuelta mediante una incidencia.",
                        List.of(new ErrorCampo("items[" + i + "].implementoId", "la unidad ya fue resuelta")));
            }
            validarIncidencia(item, i);
            seleccionados.put(item.implementoId(), detalle);
        }

        List<UUID> ids = seleccionados.keySet().stream().sorted().toList();
        Map<UUID, ImplementoDeportivo> unidades = implementos.bloquearPorIds(ids).stream()
                .collect(Collectors.toMap(ImplementoDeportivo::getId, Function.identity()));
        Usuario prestador = usuarios.findByIdAndRol(operador.id(), Rol.PRESTADOR)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.SESION_INVALIDA));
        Instant ahora = reloj.instant();
        boolean completa = prestamo.getDetalles().stream()
                .filter(detalle -> detalle.getEstado() == EstadoDetallePrestamo.PENDIENTE_DEVOLUCION)
                .count() == solicitud.items().size();
        boolean retrasoJustificado = Boolean.TRUE.equals(solicitud.retrasoJustificado());
        Devolucion devolucion = new Devolucion(prestamo, prestador, ahora,
                completa ? AlcanceDevolucion.COMPLETA : AlcanceDevolucion.PARCIAL,
                retrasoJustificado, solicitud.justificacion(), solicitud.observaciones());
        List<Incidencia> incidenciasNuevas = new ArrayList<>();
        List<HistorialEstadoImplemento> eventos = new ArrayList<>();

        for (ItemDevolucionSolicitud item : solicitud.items()) {
            DetallePrestamo detalle = seleccionados.get(item.implementoId());
            ImplementoDeportivo unidad = unidades.get(item.implementoId());
            if (unidad == null) {
                throw new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO,
                        "La unidad solicitada no existe.");
            }
            Incidencia incidencia = crearIncidencia(item.incidencia(), detalle, prestador, ahora);
            if (incidencia != null) {
                incidenciasNuevas.add(incidencia);
            }
            detalle.registrarDevolucion(ahora, item.condicion());
            EstadoImplemento anterior = unidad.getEstado();
            EstadoImplemento nuevo = item.condicion() == CondicionDevolucion.BUENO
                    ? EstadoImplemento.DISPONIBLE : EstadoImplemento.EN_MANTENIMIENTO;
            unidad.cambiarEstado(nuevo, item.condicion() == CondicionDevolucion.BUENO
                    ? EventoEstadoImplemento.DEVOLUCION : EventoEstadoImplemento.INCIDENCIA);
            eventos.add(new HistorialEstadoImplemento(unidad, anterior, nuevo,
                    item.condicion() == CondicionDevolucion.BUENO
                            ? EventoEstadoImplemento.DEVOLUCION : EventoEstadoImplemento.INCIDENCIA,
                    item.condicion() == CondicionDevolucion.BUENO
                            ? "Unidad recibida en buen estado." : "Unidad recibida con novedad."));
            devolucion.agregarDetalle(detalle, item.condicion(), item.observaciones(), incidencia);
        }

        if (!incidenciasNuevas.isEmpty()) {
            incidencias.saveAllAndFlush(incidenciasNuevas);
        }
        devoluciones.saveAndFlush(devolucion);
        implementos.saveAll(unidades.values());
        historial.saveAll(eventos);
        prestamo.actualizarEstadoTrasDevolucion(ahora);
        prestamos.saveAndFlush(prestamo);

        Sancion sancion = crearSancionSiAplica(prestamo, devolucion, ahora);
        if (sancion != null) {
            sanciones.saveAndFlush(sancion);
            prestamo.getUsuario().extenderSuspensionHasta(sancion.getFechaFin());
            usuarios.saveAndFlush(prestamo.getUsuario());
        }
        return respuesta(devolucion, sancion);
    }

    @Transactional(readOnly = true)
    public PaginaRespuesta<DevolucionResumenRespuesta> listar(DevolucionFiltros filtros, ParametrosPagina pagina) {
        if (filtros.desde() != null && filtros.hasta() != null && !filtros.desde().isBefore(filtros.hasta())) {
            throw new ExcepcionDominio(CodigoError.VALIDACION_FALLIDA,
                    "El intervalo de fechas debe ser [desde, hasta).",
                    List.of(new ErrorCampo("desde", "debe ser anterior a hasta")));
        }
        var pageable = pagina.aPageable(CAMPOS_ORDEN, Sort.by(Sort.Order.desc("fechaDevolucion"),
                Sort.Order.asc("id")));
        Page<Devolucion> resultado = devoluciones.findAll(DevolucionEspecificaciones.porFiltros(filtros), pageable);
        List<UUID> ids = resultado.getContent().stream().map(Devolucion::getId).toList();
        Map<UUID, DevolucionRepositorio.ConteoDetalles> conteos = ids.isEmpty() ? Map.of()
                : devoluciones.contarDetalles(ids, CondicionDevolucion.CON_NOVEDAD).stream()
                        .collect(Collectors.toMap(DevolucionRepositorio.ConteoDetalles::getDevolucionId,
                                Function.identity()));
        Map<UUID, List<ReferenciaImplemento>> implementosPorDevolucion = ids.isEmpty() ? Map.of()
                : devoluciones.implementosDevolucion(ids).stream().collect(Collectors.groupingBy(
                        DevolucionRepositorio.ImplementoDevuelto::getDevolucionId,
                        Collectors.mapping(implemento -> new ReferenciaImplemento(implemento.getImplementoId(),
                                implemento.getCodigoInventario(), implemento.getNombre(), null), Collectors.toList())));
        return PaginaRespuesta.de(resultado, devolucion -> {
            var conteo = conteos.get(devolucion.getId());
            return new DevolucionResumenRespuesta(devolucion.getId(), devolucion.getCodigo(),
                    referenciaPrestamo(devolucion.getPrestamo()), referenciaUsuario(devolucion.getPrestamo().getUsuario(), true),
                    fechas.aRespuesta(devolucion.getFechaDevolucion()), devolucion.getAlcance(),
                    conteo == null ? 0 : (int) conteo.getCantidad(), conteo != null && conteo.getConNovedad() > 0,
                    implementosPorDevolucion.getOrDefault(devolucion.getId(), List.of()));
        });
    }

    @Transactional(readOnly = true)
    public DevolucionRespuesta consultar(UUID id) {
        Devolucion devolucion = devoluciones.findById(id)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO));
        return respuesta(devolucion, sanciones.findByDevolucionId(id).orElse(null));
    }

    private Sancion crearSancionSiAplica(Prestamo prestamo, Devolucion devolucion, Instant ahora) {
        ContextoSancion contexto = new ContextoSancion(prestamo, devolucion, ahora);
        return politicasSancion.stream()
                .filter(politica -> politica.aplica(contexto))
                .findFirst()
                .flatMap(politica -> politica.calcular(contexto))
                .map(resultado -> new Sancion(prestamo.getUsuario(), prestamo, devolucion,
                        "Devolución tardía sin justificación", resultado.minutosRetraso(), ahora,
                        ahora.plus(resultado.duracion()), null))
                .orElse(null);
    }

    private Incidencia crearIncidencia(IncidenciaEnDevolucionSolicitud solicitud, DetallePrestamo detalle,
            Usuario prestador, Instant ahora) {
        return solicitud == null ? null : new Incidencia(detalle, prestador, solicitud.tipo(), solicitud.descripcion(), ahora);
    }

    private void validarIncidencia(ItemDevolucionSolicitud item, int indice) {
        if (item.condicion() == CondicionDevolucion.CON_NOVEDAD && item.incidencia() == null) {
            throw new ExcepcionDominio(CodigoError.INCIDENCIA_REQUERIDA,
                    "La unidad " + item.implementoId() + " se recibió con novedad y requiere una incidencia.",
                    List.of(new ErrorCampo("items[" + indice + "].incidencia", "es obligatoria cuando la condición es CON_NOVEDAD")));
        }
        if (item.condicion() == CondicionDevolucion.BUENO && item.incidencia() != null) {
            throw new ExcepcionDominio(CodigoError.TIPO_INCIDENCIA_NO_PERMITIDO,
                    "Una unidad recibida en buen estado no puede registrar una incidencia.",
                    List.of(new ErrorCampo("items[" + indice + "].incidencia", "no aplica cuando la condición es BUENO")));
        }
        if (item.incidencia() != null && item.incidencia().tipo() != TipoIncidencia.DANO
                && item.incidencia().tipo() != TipoIncidencia.OTRA) {
            throw new ExcepcionDominio(CodigoError.TIPO_INCIDENCIA_NO_PERMITIDO,
                    "Las incidencias de una devolución solo pueden ser DANO u OTRA.",
                    List.of(new ErrorCampo("items[" + indice + "].incidencia.tipo", "debe ser DANO u OTRA")));
        }
    }

    private void validarIdsNoRepetidos(List<ItemDevolucionSolicitud> items) {
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            if (!ids.add(items.get(i).implementoId())) {
                throw new ExcepcionDominio(CodigoError.SOLICITUD_INVALIDA,
                        "Una unidad no puede aparecer más de una vez en la devolución.",
                        List.of(new ErrorCampo("items[" + i + "].implementoId", "está repetido en la solicitud")));
            }
        }
    }

    private void exigirPrestador(UsuarioAutenticado operador) {
        if (operador == null || operador.rol() != Rol.PRESTADOR) {
            throw new ExcepcionDominio(CodigoError.ACCESO_DENEGADO);
        }
    }

    private DevolucionRespuesta respuesta(Devolucion devolucion, Sancion sancion) {
        Prestamo prestamo = devolucion.getPrestamo();
        List<ItemDevolucionRespuesta> items = devolucion.getDetalles().stream().map(this::itemRespuesta).toList();
        return new DevolucionRespuesta(devolucion.getId(), devolucion.getCodigo(), referenciaPrestamo(prestamo),
                referenciaUsuario(prestamo.getUsuario(), true), referenciaUsuario(devolucion.getPrestador(), false),
                fechas.aRespuesta(devolucion.getFechaDevolucion()), devolucion.getAlcance(),
                devolucion.isRetrasoJustificado(), devolucion.getJustificacion(), devolucion.getObservaciones(), items,
                sancion == null ? null : new SancionAplicadaRespuesta(fechas.aRespuesta(sancion.getFechaInicio()),
                        fechas.aRespuesta(sancion.getFechaFin()), sancion.getMinutosRetraso()));
    }

    private ItemDevolucionRespuesta itemRespuesta(DetalleDevolucion detalle) {
        Incidencia incidencia = detalle.getIncidencia();
        return new ItemDevolucionRespuesta(
                new ReferenciaImplemento(detalle.getDetallePrestamo().getImplemento().getId(),
                        detalle.getDetallePrestamo().getImplemento().getCodigoInventario(),
                        detalle.getDetallePrestamo().getImplemento().getNombre(),
                        detalle.getDetallePrestamo().getImplemento().getEstado()),
                detalle.getCondicion(), detalle.getObservaciones(),
                detalle.getDetallePrestamo().getImplemento().getEstado(),
                incidencia == null ? null : new ReferenciaIncidencia(incidencia.getId(), incidencia.getCodigo(),
                        incidencia.getTipo(), incidencia.getImplemento().getCodigoInventario()));
    }

    private ReferenciaPrestamo referenciaPrestamo(Prestamo prestamo) {
        EstadoPrestamo estado = prestamo.getEstado();
        if (estado != EstadoPrestamo.FINALIZADO && !prestamo.getFechaLimiteDevolucion().isAfter(reloj.instant())) {
            estado = EstadoPrestamo.VENCIDO;
        }
        return new ReferenciaPrestamo(prestamo.getId(), prestamo.getCodigo(), estado);
    }

    private static ReferenciaUsuario referenciaUsuario(Usuario usuario, boolean identificacion) {
        return new ReferenciaUsuario(usuario.getId(), usuario.getNombres(), usuario.getApellidos(),
                identificacion ? usuario.getIdentificacion() : null);
    }
}
