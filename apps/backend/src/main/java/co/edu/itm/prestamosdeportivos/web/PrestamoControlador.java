package co.edu.itm.prestamosdeportivos.web;

import java.net.URI;
import co.edu.itm.prestamosdeportivos.aplicacion.PrestamoServicio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.CrearPrestamoSolicitud;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.PrestamoRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-14: entrega de unidades al solicitante. */
@RestController
@RequestMapping("/api/v1")
public class PrestamoControlador {
    private final PrestamoServicio servicio;
    public PrestamoControlador(PrestamoServicio servicio) { this.servicio = servicio; }

    @PostMapping({"/prestamos", "/loans"})
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Registrar un préstamo", description = "Registra atómicamente la entrega y bloquea las unidades solicitadas.")
    public ResponseEntity<PrestamoRespuesta> registrar(@Valid @RequestBody CrearPrestamoSolicitud solicitud,
            @AuthenticationPrincipal UsuarioAutenticado operador) {
        var respuesta = servicio.registrar(solicitud.usuarioId(), solicitud.implementoIds(), operador);
        return ResponseEntity.created(URI.create("/api/v1/prestamos/" + respuesta.id())).body(respuesta);
    }
}
