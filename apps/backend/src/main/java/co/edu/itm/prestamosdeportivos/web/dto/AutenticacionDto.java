package co.edu.itm.prestamosdeportivos.web.dto;

import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.MAXIMO_CARACTERES;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.OBLIGATORIO;

import java.time.OffsetDateTime;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CU-01 y CU-02 (BE-01 §10.1). */
public final class AutenticacionDto {

    private AutenticacionDto() {
    }

    public record SolicitudLogin(
            @Schema(description = "Correo o identificación.", example = "maria.rodriguez@institucion.edu.co")
            @NotBlank(message = OBLIGATORIO) @Size(max = 254, message = MAXIMO_CARACTERES)
            String identificador,
            @Schema(example = "S3gura!2026", format = "password")
            @NotBlank(message = OBLIGATORIO) @Size(max = 72, message = MAXIMO_CARACTERES)
            String contrasena) {

        /** Evita que la contraseña aparezca en logs si el record se imprime. */
        @Override
        public String toString() {
            return "SolicitudLogin[identificador=" + identificador + ", contrasena=***]";
        }
    }

    public record UsuarioSesion(UUID id, String nombres, String apellidos, String correo, Rol rol) {
    }

    public record RespuestaLogin(
            @Schema(description = "JWT para la cabecera Authorization: Bearer.") String tokenAcceso,
            @Schema(example = "Bearer") String tipoToken,
            OffsetDateTime expiraEn,
            UsuarioSesion usuario) {

        @Override
        public String toString() {
            return "RespuestaLogin[tokenAcceso=***, tipoToken=" + tipoToken + ", expiraEn=" + expiraEn
                    + ", usuario=" + usuario + "]";
        }
    }
}
