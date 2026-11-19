package co.edu.itm.prestamosdeportivos.web.dto;

import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.MAXIMO_CARACTERES;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.OBLIGATORIO;

import java.time.OffsetDateTime;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoIncidencia;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaImplemento;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaPrestamo;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CU-18 y CU-19 (BE-01 §10.8). */
public final class IncidenciaDto {

    private IncidenciaDto() {
    }

    public record RegistrarIncidenciaSolicitud(
            @NotNull(message = OBLIGATORIO) UUID implementoId,
            @Schema(description = "PERDIDA o ROBO (unidad no devuelta).") @NotNull(message = OBLIGATORIO)
            TipoIncidencia tipo,
            @Schema(example = "El usuario reporta que el peto se extravió en la cancha 2.")
            @NotBlank(message = OBLIGATORIO) @Size(max = 1000, message = MAXIMO_CARACTERES) String descripcion) {
    }

    public record IncidenciaRespuesta(UUID id, String codigo, TipoIncidencia tipo, EstadoIncidencia estado,
            String descripcion, OffsetDateTime fechaRegistro, ReferenciaPrestamo prestamo,
            ReferenciaImplemento implemento, ReferenciaUsuario usuario, ReferenciaUsuario prestador) {
    }
}
