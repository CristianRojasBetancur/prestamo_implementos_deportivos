package co.edu.itm.prestamosdeportivos.web;

import java.util.UUID;
import co.edu.itm.prestamosdeportivos.aplicacion.SituacionUsuarioServicio;
import co.edu.itm.prestamosdeportivos.web.dto.SituacionUsuarioDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CU-13 / HU-11: consulta informativa de elegibilidad del solicitante. */
@RestController
@RequestMapping("/api/v1")
public class SituacionUsuarioControlador {
    private final SituacionUsuarioServicio servicio;
    public SituacionUsuarioControlador(SituacionUsuarioServicio servicio){this.servicio=servicio;}
    @GetMapping("/usuarios/{userId}/situacion")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary="Consultar situación de préstamo del usuario",description="Informativa; no modifica datos y siempre vuelve a validar al registrar un préstamo.")
    public SituacionUsuarioDto.Respuesta consultar(@PathVariable UUID userId){return servicio.consultar(userId);}
}
