package co.edu.itm.prestamosdeportivos.aplicacion;

import static co.edu.itm.prestamosdeportivos.web.dto.IncidenciaDto.IncidenciaRespuesta;
import static co.edu.itm.prestamosdeportivos.web.dto.IncidenciaDto.IncidenciaResumenRespuesta;
import static co.edu.itm.prestamosdeportivos.web.dto.IncidenciaDto.RegistrarIncidenciaSolicitud;
import static co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaImplemento;
import static co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaPrestamo;
import static co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.Incidencia;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.dominio.DetallePrestamo;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaEspecificaciones;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaFiltros;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import co.edu.itm.prestamosdeportivos.web.dto.IncidenciaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fachada transaccional de CU-18 y CU-19, incluida la resolución administrativa. */
@Service
public class IncidenciaServicio {

    private static final Set<String> CAMPOS_ORDEN = Set.of("fechaRegistro", "tipo", "estado", "codigo");

    private final IncidenciaRepositorio incidencias;
    private final PrestamoRepositorio prestamos;
    private final ImplementoDeportivoRepositorio implementos;
    private final IncidenciaDisponibilidad disponibilidad;
    private final CierrePrestamo cierre;
    private final UsuarioRepositorio usuarios;
    private final ConversorFechas fechas;
    private final Clock reloj;

    public IncidenciaServicio(IncidenciaRepositorio incidencias, PrestamoRepositorio prestamos,
            ImplementoDeportivoRepositorio implementos, IncidenciaDisponibilidad disponibilidad,
            CierrePrestamo cierre,
            UsuarioRepositorio usuarios, ConversorFechas fechas, Clock reloj) {
        this.incidencias = incidencias;
        this.prestamos = prestamos;
        this.implementos = implementos;
        this.disponibilidad = disponibilidad;
        this.cierre = cierre;
        this.usuarios = usuarios;
        this.fechas = fechas;
        this.reloj = reloj;
    }

    @Transactional
    public IncidenciaRespuesta registrar(UUID prestamoId, RegistrarIncidenciaSolicitud solicitud,
            UsuarioAutenticado operador) {
        exigirRol(operador, Rol.PRESTADOR);
        if (solicitud.tipo() != TipoIncidencia.PERDIDA && solicitud.tipo() != TipoIncidencia.ROBO) {
            throw new ExcepcionDominio(CodigoError.TIPO_INCIDENCIA_NO_PERMITIDO,
                    "Las incidencias independientes solo pueden ser PERDIDA o ROBO.",
                    List.of(new ErrorCampo("tipo", "debe ser PERDIDA o ROBO")));
        }

        Prestamo prestamo = prestamos.bloquearPorId(prestamoId)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO,
                        "El préstamo solicitado no existe."));
        if (prestamo.getEstado() == EstadoPrestamo.FINALIZADO) {
            throw new ExcepcionDominio(CodigoError.PRESTAMO_FINALIZADO);
        }
        DetallePrestamo detalle = prestamo.getDetalles().stream()
                .filter(item -> item.getImplemento().getId().equals(solicitud.implementoId()))
                .findFirst()
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO,
                        "El implemento no pertenece al préstamo indicado.",
                        List.of(new ErrorCampo("implementoId", "no pertenece al préstamo"))));
        if (detalle.getEstado() != EstadoDetallePrestamo.PENDIENTE_DEVOLUCION) {
            throw new ExcepcionDominio(CodigoError.UNIDAD_YA_RESUELTA);
        }

        ImplementoDeportivo implemento = implementos.bloquearPorIds(List.of(solicitud.implementoId())).stream()
                .findFirst()
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO,
                        "La unidad solicitada no existe."));
        Usuario prestador = usuarios.findByIdAndRol(operador.id(), Rol.PRESTADOR)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.SESION_INVALIDA));
        Instant ahora = reloj.instant();
        Incidencia incidencia = new Incidencia(detalle, prestador, solicitud.tipo(), solicitud.descripcion().trim(), ahora);
        detalle.registrarIncidencia(ahora);
        disponibilidad.marcarNoDisponible(implemento, EstadoImplemento.DADO_DE_BAJA, prestador,
                "Unidad reportada como " + solicitud.tipo().name() + ".");
        incidencias.save(incidencia);
        cierre.actualizar(prestamo, ahora);
        prestamos.saveAndFlush(prestamo);
        return respuesta(incidencia);
    }

    @Transactional(readOnly = true)
    public PaginaRespuesta<IncidenciaResumenRespuesta> listar(IncidenciaFiltros filtros, ParametrosPagina pagina) {
        if (filtros.desde() != null && filtros.hasta() != null && !filtros.desde().isBefore(filtros.hasta())) {
            throw new ExcepcionDominio(CodigoError.VALIDACION_FALLIDA,
                    "El intervalo de fechas debe ser [desde, hasta).",
                    List.of(new ErrorCampo("desde", "debe ser anterior a hasta")));
        }
        var pageable = pagina.aPageable(CAMPOS_ORDEN,
                Sort.by(Sort.Order.desc("fechaRegistro"), Sort.Order.asc("id")));
        Page<Incidencia> resultado = incidencias.findAll(IncidenciaEspecificaciones.porFiltros(filtros), pageable);
        return PaginaRespuesta.de(resultado, this::resumen);
    }

    @Transactional(readOnly = true)
    public IncidenciaRespuesta consultar(UUID id) {
        return respuesta(incidencias.findById(id)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO)));
    }

    @Transactional
    public IncidenciaRespuesta resolver(UUID id, UsuarioAutenticado operador) {
        exigirRol(operador, Rol.ADMIN);
        Incidencia incidencia = incidencias.bloquearPorId(id)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO));
        Usuario administrador = usuarios.findByIdAndRol(operador.id(), Rol.ADMIN)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.SESION_INVALIDA));
        incidencia.resolver(administrador, reloj.instant());
        incidencias.saveAndFlush(incidencia);
        return respuesta(incidencia);
    }

    private IncidenciaRespuesta respuesta(Incidencia incidencia) {
        return new IncidenciaRespuesta(incidencia.getId(), incidencia.getCodigo(), incidencia.getTipo(),
                incidencia.getEstado(), incidencia.getDescripcion(), fechas.aRespuesta(incidencia.getFechaRegistro()),
                fechas.aRespuesta(incidencia.getFechaResolucion()), referenciaPrestamo(incidencia.getPrestamo()),
                referenciaImplemento(incidencia.getImplemento()), referenciaUsuario(incidencia.getPrestamo().getUsuario(), true),
                referenciaUsuario(incidencia.getPrestador(), false),
                incidencia.getResueltoPor() == null ? null : referenciaUsuario(incidencia.getResueltoPor(), false));
    }

    private IncidenciaResumenRespuesta resumen(Incidencia incidencia) {
        return new IncidenciaResumenRespuesta(incidencia.getId(), incidencia.getCodigo(), incidencia.getTipo(),
                incidencia.getEstado(), fechas.aRespuesta(incidencia.getFechaRegistro()),
                fechas.aRespuesta(incidencia.getFechaResolucion()), referenciaPrestamo(incidencia.getPrestamo()),
                referenciaImplemento(incidencia.getImplemento()), referenciaUsuario(incidencia.getPrestamo().getUsuario(), false));
    }

    private static ReferenciaPrestamo referenciaPrestamo(Prestamo prestamo) {
        return new ReferenciaPrestamo(prestamo.getId(), prestamo.getCodigo(), prestamo.getEstado());
    }

    private static ReferenciaImplemento referenciaImplemento(ImplementoDeportivo implemento) {
        return new ReferenciaImplemento(implemento.getId(), implemento.getCodigoInventario(), implemento.getNombre(),
                implemento.getEstado());
    }

    private static ReferenciaUsuario referenciaUsuario(Usuario usuario, boolean identificacion) {
        return new ReferenciaUsuario(usuario.getId(), usuario.getNombres(), usuario.getApellidos(),
                identificacion ? usuario.getIdentificacion() : null);
    }

    private static void exigirRol(UsuarioAutenticado operador, Rol rol) {
        if (operador == null || operador.rol() != rol) {
            throw new ExcepcionDominio(CodigoError.ACCESO_DENEGADO);
        }
    }
}
