package co.edu.itm.prestamosdeportivos.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.DetallePrestamoRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.PrestamoRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaImplemento;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Facade transaccional de CU-14 (BE-01) y aliases BE-10. */
@Service
public class PrestamoServicio {
    private final PrestamoRepositorio prestamos;
    private final ImplementoDeportivoRepositorio implementos;
    private final UsuarioRepositorio usuarios;
    private final ReservaDisponibilidad disponibilidad;
    private final SituacionUsuarioServicio elegibilidad;
    private final ConversorFechas conversorFechas;
    private final Clock reloj;

    public PrestamoServicio(PrestamoRepositorio prestamos, ImplementoDeportivoRepositorio implementos,
            UsuarioRepositorio usuarios, SituacionUsuarioServicio elegibilidad,
            ConversorFechas conversorFechas, Clock reloj, ReservaDisponibilidad disponibilidad) {
        this.prestamos = prestamos;
        this.implementos = implementos;
        this.usuarios = usuarios;
        this.disponibilidad = disponibilidad;
        this.elegibilidad = elegibilidad;
        this.conversorFechas = conversorFechas;
        this.reloj = reloj;
    }

    @Transactional
    public PrestamoRespuesta registrar(UUID usuarioId, List<UUID> implementoIds, UsuarioAutenticado operador) {
        if (operador == null || operador.rol() != Rol.PRESTADOR) {
            throw new ExcepcionDominio(CodigoError.ACCESO_DENEGADO);
        }
        Usuario usuarioBloqueado = usuarios.bloquearPorIdAndRol(usuarioId, Rol.USER)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO,
                        "El usuario solicitante no existe o no tiene rol USER.",
                        List.of(new ErrorCampo("usuarioId", "no existe un usuario solicitante con ese identificador"))));
        List<UUID> ids = implementoIds == null ? List.of() : implementoIds;
        // SELECT FOR UPDATE ordenado evita doble asignación y deadlocks entre lotes.
        List<co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo> bloqueadas =
                ids.isEmpty() ? List.of() : implementos.bloquearPorIds(ids);
        Map<UUID, co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo> porId = bloqueadas.stream()
                .collect(Collectors.toMap(i -> i.getId(), Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<ErrorCampo> ausentes = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            if (!porId.containsKey(ids.get(i))) {
                ausentes.add(new ErrorCampo("implementoIds[" + i + "]", "la unidad no existe"));
            }
        }
        if (!ausentes.isEmpty()) {
            throw new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO,
                    "Una o más unidades solicitadas no existen.", ausentes);
        }

        // Esta vuelve a ejecutar BE-09 dentro de la transacción y con las filas ya bloqueadas.
        elegibilidad.validarSolicitudParaPrestamo(usuarioId, ids);
        Usuario usuario = usuarioBloqueado;
        Usuario prestador = usuarios.findByIdAndRol(operador.id(), Rol.PRESTADOR)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.SESION_INVALIDA));

        var entrega = reloj.instant();
        Prestamo prestamo = new Prestamo(usuario, prestador, entrega, entrega.plus(Duration.ofHours(24)));
        List<co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo> unidades = new ArrayList<>();
        for (UUID id : new java.util.LinkedHashSet<>(ids)) {
            var unidad = porId.get(id);
            unidades.add(unidad);
            prestamo.agregarDetalle(unidad);
        }
        disponibilidad.reservar(unidades, prestador);
        var guardado = prestamos.saveAndFlush(prestamo);
        return respuesta(guardado);
    }

    private PrestamoRespuesta respuesta(Prestamo prestamo) {
        var detalles = prestamo.getDetalles().stream().map(detalle -> new DetallePrestamoRespuesta(
                detalle.getId(), new ReferenciaImplemento(detalle.getImplemento().getId(),
                        detalle.getImplemento().getCodigoInventario(), detalle.getImplemento().getNombre(), null),
                detalle.getEstado())).toList();
        return new PrestamoRespuesta(prestamo.getId(), prestamo.getCodigo(), EstadoPrestamo.VIGENTE,
                referencia(prestamo.getUsuario(), true), referencia(prestamo.getPrestador(), false),
                conversorFechas.aRespuesta(prestamo.getFechaPrestamo()),
                conversorFechas.aRespuesta(prestamo.getFechaLimiteDevolucion()),
                conversorFechas.aRespuesta(prestamo.getFechaFinalizacion()), false, detalles.size(), detalles,
                List.of(), List.of());
    }

    private static ReferenciaUsuario referencia(Usuario usuario, boolean conIdentificacion) {
        return new ReferenciaUsuario(usuario.getId(), usuario.getNombres(), usuario.getApellidos(),
                conIdentificacion ? usuario.getIdentificacion() : null);
    }
}
