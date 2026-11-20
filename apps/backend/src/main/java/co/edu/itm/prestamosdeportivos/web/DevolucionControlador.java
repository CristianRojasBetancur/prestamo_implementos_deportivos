package co.edu.itm.prestamosdeportivos.web;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.aplicacion.DevolucionServicio;
import co.edu.itm.prestamosdeportivos.dominio.AlcanceDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
import co.edu.itm.prestamosdeportivos.persistencia.DevolucionFiltros;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import io.swagger.v3.oas.annotations.Operation;
import co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.DevolucionRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.DevolucionResumenRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.RegistrarDevolucionSolicitud;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/** CU-16 y CU-17: registro y consulta de devoluciones. */
@RestController
@Validated
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('PRESTADOR')")
public class DevolucionControlador {

    private final DevolucionServicio servicio;

    public DevolucionControlador(DevolucionServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/prestamos/{prestamoId}/devoluciones")
    public ResponseEntity<DevolucionRespuesta> registrar(@PathVariable UUID prestamoId,
            @Valid @RequestBody RegistrarDevolucionSolicitud solicitud,
            @AuthenticationPrincipal UsuarioAutenticado operador) {
        DevolucionRespuesta respuesta = servicio.registrar(prestamoId, solicitud, operador);
        return ResponseEntity.created(URI.create("/api/v1/devoluciones/" + respuesta.id())).body(respuesta);
    }

    @GetMapping("/devoluciones")
    @Operation(summary = "Consultar devoluciones", description = "Lista devoluciones con sus relaciones de préstamo, usuario, unidad, fecha y condición.")
    public PaginaRespuesta<DevolucionResumenRespuesta> listar(
            @RequestParam(required = false) UUID prestamoId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) UUID implementoId,
            @RequestParam(required = false) CondicionDevolucion condicion,
            @RequestParam(required = false) AlcanceDevolucion alcance,
            @RequestParam(required = false) OffsetDateTime desde,
            @RequestParam(required = false) OffsetDateTime hasta,
            @RequestParam(required = false, name = "q") @Size(max = 80, message = "no puede superar 80 caracteres") String texto,
            @Valid @ParameterObject ParametrosPagina pagina) {
        return servicio.listar(new DevolucionFiltros(prestamoId, usuarioId, implementoId, condicion, alcance,
                desde == null ? null : desde.toInstant(), hasta == null ? null : hasta.toInstant(), texto), pagina);
    }

    @GetMapping("/devoluciones/{id}")
    @Operation(summary = "Consultar detalle de devolución", description = "Consulta una devolución y su información de sanción sin modificarla.")
    public DevolucionRespuesta consultar(@PathVariable UUID id) {
        return servicio.consultar(id);
    }
}
