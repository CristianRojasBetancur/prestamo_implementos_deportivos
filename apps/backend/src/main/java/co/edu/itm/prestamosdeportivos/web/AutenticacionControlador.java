package co.edu.itm.prestamosdeportivos.web;

import co.edu.itm.prestamosdeportivos.aplicacion.AutenticacionServicio;
import co.edu.itm.prestamosdeportivos.aplicacion.SesionEmitida;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import co.edu.itm.prestamosdeportivos.web.dto.AutenticacionDto.RespuestaLogin;
import co.edu.itm.prestamosdeportivos.web.dto.AutenticacionDto.SolicitudLogin;
import co.edu.itm.prestamosdeportivos.web.dto.AutenticacionDto.UsuarioSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CU-01, CU-02 y cierre de sesión (BE-01 §5 y §10.1). */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT, sesión actual y cierre de sesión.")
public class AutenticacionControlador {

    private static final String TIPO_TOKEN = "Bearer";

    private final AutenticacionServicio servicio;
    private final ConversorFechas conversorFechas;

    public AutenticacionControlador(AutenticacionServicio servicio, ConversorFechas conversorFechas) {
        this.servicio = servicio;
        this.conversorFechas = conversorFechas;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = "Solo para PRESTADOR y ADMIN activos. Cualquier fallo responde 401 CREDENCIALES_INVALIDAS "
                    + "sin indicar si falló el usuario o la contraseña. El rol lo determina el servidor.")
    public RespuestaLogin iniciarSesion(@Valid @RequestBody SolicitudLogin solicitud) {
        SesionEmitida sesion = servicio.iniciarSesion(solicitud.identificador(), solicitud.contrasena());
        return new RespuestaLogin(sesion.token().valor(), TIPO_TOKEN,
                conversorFechas.aRespuesta(sesion.token().expiraEn()), aSesion(sesion.usuario()));
    }

    @GetMapping("/me")
    @Operation(summary = "Sesión actual", description = "Usuario autenticado y su rol, para mostrar el menú por rol.")
    public UsuarioSesion sesionActual(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return aSesion(servicio.usuarioActual(autenticado.id()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar sesión",
            description = "Revoca todas las sesiones del usuario: los tokens emitidos dejan de ser válidos (401 SESION_INVALIDA).")
    public void cerrarSesion(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        servicio.cerrarSesion(autenticado.id());
    }

    private static UsuarioSesion aSesion(Usuario usuario) {
        return new UsuarioSesion(usuario.getId(), usuario.getNombres(), usuario.getApellidos(), usuario.getCorreo(),
                usuario.getRol());
    }
}
