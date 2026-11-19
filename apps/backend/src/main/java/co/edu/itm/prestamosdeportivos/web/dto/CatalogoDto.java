package co.edu.itm.prestamosdeportivos.web.dto;

import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.MAXIMO_CARACTERES;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.OBLIGATORIO;

import java.util.UUID;

import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaCategoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CU-07 y CU-12 (BE-01 §10.2). */
public final class CatalogoDto {

    private CatalogoDto() {
    }

    @Schema(description = "Disponibilidad agrupada por (categoría, nombre). No expone unidades ni códigos.")
    public record DisponibilidadRespuesta(
            @Schema(example = "Balón de fútbol") String nombre,
            ReferenciaCategoria categoria,
            @Schema(example = "3") long unidadesDisponibles,
            @Schema(example = "5") long unidadesTotales,
            @Schema(example = "true") boolean disponible) {
    }

    public record CategoriaRespuesta(UUID id, String nombre, String descripcion, boolean activa) {
    }

    public record CategoriaSolicitud(
            @Schema(example = "Balones")
            @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = MAXIMO_CARACTERES)
            String nombre,
            @Size(max = 500, message = MAXIMO_CARACTERES)
            String descripcion,
            @Schema(description = "Si se omite al crear, la categoría queda activa.")
            Boolean activa) {
    }
}
