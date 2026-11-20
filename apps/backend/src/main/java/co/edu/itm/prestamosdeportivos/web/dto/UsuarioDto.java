package co.edu.itm.prestamosdeportivos.web.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CU-13: búsqueda de solicitantes y situación de préstamo (BE-01 §10.5). */
public final class UsuarioDto {

    private UsuarioDto() {
    }

    public record SolicitanteRespuesta(UUID id, String nombres, String apellidos, String identificacion,
            boolean activo) {
    }

    public record CrearSolicitanteSolicitud(
            @NotBlank(message = "es obligatorio") @Size(max = 40, message = "no puede superar 40 caracteres")
            String identificacion,
            @NotBlank(message = "es obligatorio") @Size(max = 120, message = "no puede superar 120 caracteres")
            String nombres,
            @NotBlank(message = "es obligatorio") @Size(max = 120, message = "no puede superar 120 caracteres")
            String apellidos) {
    }

    public record MotivoInhabilitacion(CodigoError codigo, String mensaje) {
    }

    public record SancionVigenteRespuesta(OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
    }

    public record SituacionUsuarioRespuesta(ReferenciaUsuario usuario, boolean habilitado,
            List<MotivoInhabilitacion> motivos, int unidadesActivas, int unidadesMaximas,
            int unidadesDisponiblesParaPrestar, int prestamosActivos, int prestamosVencidos,
            SancionVigenteRespuesta sancionVigente) {
    }
}
