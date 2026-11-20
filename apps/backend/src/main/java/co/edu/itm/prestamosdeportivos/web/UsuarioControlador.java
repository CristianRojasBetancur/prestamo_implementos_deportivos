package co.edu.itm.prestamosdeportivos.web;

import java.net.URI;

import co.edu.itm.prestamosdeportivos.aplicacion.UsuarioRegistroServicio;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

import org.springdoc.core.annotations.ParameterObject;

import co.edu.itm.prestamosdeportivos.aplicacion.UsuarioConsultaServicio;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import co.edu.itm.prestamosdeportivos.web.dto.UsuarioDto.CrearSolicitanteSolicitud;
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
    private final UsuarioRegistroServicio registro;

    public UsuarioControlador(UsuarioConsultaServicio servicio, UsuarioRegistroServicio registro) {
        this.servicio = servicio;
        this.registro = registro;
    }

    @PostMapping
    @Operation(summary = "Registrar usuario solicitante",
            description = "Crea un usuario USER activo para continuar la preparación de un préstamo.")
    public ResponseEntity<SolicitanteRespuesta> registrar(@Valid @RequestBody CrearSolicitanteSolicitud solicitud) {
        SolicitanteRespuesta creado = registro.registrar(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.id())).body(creado);
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
