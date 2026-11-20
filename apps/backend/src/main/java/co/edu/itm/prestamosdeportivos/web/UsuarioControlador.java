package co.edu.itm.prestamosdeportivos.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

import org.springdoc.core.annotations.ParameterObject;

import co.edu.itm.prestamosdeportivos.aplicacion.UsuarioConsultaServicio;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import co.edu.itm.prestamosdeportivos.web.dto.UsuarioDto.SolicitanteRespuesta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/** CU-13: búsqueda paginada de usuarios solicitantes. */
@RestController
@Validated
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('PRESTADOR')")
public class UsuarioControlador {

    private final UsuarioConsultaServicio servicio;

    public UsuarioControlador(UsuarioConsultaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Buscar usuarios solicitantes",
            description = "Listado paginado de usuarios con rol USER. La consulta es informativa y no modifica datos.")
    public PaginaRespuesta<SolicitanteRespuesta> listar(
            @RequestParam(required = false, name = "q") @Size(max = 80, message = "no puede superar 80 caracteres") String texto,
            @Valid @ParameterObject ParametrosPagina pagina) {
        return servicio.listar(texto, pagina);
    }
}
