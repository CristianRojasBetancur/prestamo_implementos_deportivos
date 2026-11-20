package co.edu.itm.prestamosdeportivos.web;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.aplicacion.IncidenciaServicio;
import co.edu.itm.prestamosdeportivos.dominio.EstadoIncidencia;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaFiltros;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import co.edu.itm.prestamosdeportivos.web.dto.IncidenciaDto.IncidenciaRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.IncidenciaDto.IncidenciaResumenRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.IncidenciaDto.RegistrarIncidenciaSolicitud;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CU-18 y CU-19: registro, consulta y resolución administrativa de incidencias. */
@RestController
@RequestMapping("/api/v1")
public class IncidenciaControlador {

    private final IncidenciaServicio servicio;

    public IncidenciaControlador(IncidenciaServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/prestamos/{prestamoId}/incidencias")
    @PreAuthorize("hasRole('PRESTADOR')")
    public ResponseEntity<IncidenciaRespuesta> registrar(@PathVariable UUID prestamoId,
            @Valid @RequestBody RegistrarIncidenciaSolicitud solicitud,
            @AuthenticationPrincipal UsuarioAutenticado operador) {
        IncidenciaRespuesta respuesta = servicio.registrar(prestamoId, solicitud, operador);
        return ResponseEntity.created(URI.create("/api/v1/incidencias/" + respuesta.id())).body(respuesta);
    }

    @GetMapping("/incidencias")
    @PreAuthorize("hasAnyRole('PRESTADOR', 'ADMIN')")
    public PaginaRespuesta<IncidenciaResumenRespuesta> listar(
            @RequestParam(required = false) UUID prestamoId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) UUID implementoId,
            @RequestParam(required = false) TipoIncidencia tipo,
            @RequestParam(required = false) EstadoIncidencia estado,
            @RequestParam(required = false) OffsetDateTime desde,
            @RequestParam(required = false) OffsetDateTime hasta,
            @RequestParam(required = false, name = "q") String texto,
            @Valid @ParameterObject ParametrosPagina pagina) {
        return servicio.listar(new IncidenciaFiltros(prestamoId, usuarioId, implementoId, tipo, estado,
                desde == null ? null : desde.toInstant(), hasta == null ? null : hasta.toInstant(), texto), pagina);
    }

    @GetMapping("/incidencias/{id}")
    @PreAuthorize("hasAnyRole('PRESTADOR', 'ADMIN')")
    public IncidenciaRespuesta consultar(@PathVariable UUID id) {
        return servicio.consultar(id);
    }

    @PatchMapping("/incidencias/{id}/resolucion")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidenciaRespuesta resolver(@PathVariable UUID id,
            @AuthenticationPrincipal UsuarioAutenticado operador) {
        return servicio.resolver(id, operador);
    }
}
