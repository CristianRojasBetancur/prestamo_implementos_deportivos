package co.edu.itm.prestamosdeportivos.web.dto;

import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.CODIGO;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.CORREO;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.MAXIMO_CARACTERES;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.OBLIGATORIO;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.PATRON_CODIGO;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.RANGO_CARACTERES;

import java.time.OffsetDateTime;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** CU-03 a CU-06 (BE-01 §10.3). */
public final class PrestadorDto {

    private PrestadorDto() {
    }

    /** Vista de {@code usuario.activo} en la API. */
    public enum EstadoCuenta {
        ACTIVO,
        INACTIVO
    }

    public record CrearPrestadorSolicitud(
            @Schema(example = "Felipe") @NotBlank(message = OBLIGATORIO) @Size(max = 100, message = MAXIMO_CARACTERES)
            String nombres,
            @Schema(example = "Gómez") @NotBlank(message = OBLIGATORIO) @Size(max = 100, message = MAXIMO_CARACTERES)
            String apellidos,
            @Schema(example = "1045678901") @NotBlank(message = OBLIGATORIO) @Size(max = 30, message = MAXIMO_CARACTERES)
            @Pattern(regexp = PATRON_CODIGO, message = CODIGO)
            String identificacion,
            @Schema(example = "felipe.gomez@institucion.edu.co") @NotBlank(message = OBLIGATORIO)
            @Email(message = CORREO) @Size(max = 254, message = MAXIMO_CARACTERES)
            String correo,
            @Schema(example = "Temporal#2026", format = "password") @NotBlank(message = OBLIGATORIO)
            @Size(min = 8, max = 72, message = RANGO_CARACTERES)
            String contrasenaInicial) {

        @Override
        public String toString() {
            return "CrearPrestadorSolicitud[identificacion=" + identificacion + ", correo=" + correo
                    + ", contrasenaInicial=***]";
        }
    }

    public record ActualizarPrestadorSolicitud(
            @NotBlank(message = OBLIGATORIO) @Size(max = 100, message = MAXIMO_CARACTERES) String nombres,
            @NotBlank(message = OBLIGATORIO) @Size(max = 100, message = MAXIMO_CARACTERES) String apellidos,
            @NotBlank(message = OBLIGATORIO) @Size(max = 30, message = MAXIMO_CARACTERES)
            @Pattern(regexp = PATRON_CODIGO, message = CODIGO) String identificacion,
            @NotBlank(message = OBLIGATORIO) @Email(message = CORREO) @Size(max = 254, message = MAXIMO_CARACTERES)
            String correo) {
    }

    public record CambioEstadoCuentaSolicitud(@NotNull(message = OBLIGATORIO) EstadoCuenta estado) {
    }

    public record PrestadorRespuesta(UUID id, String nombres, String apellidos, String identificacion, String correo,
            Rol rol, EstadoCuenta estado, OffsetDateTime creadoEn) {
    }
}
