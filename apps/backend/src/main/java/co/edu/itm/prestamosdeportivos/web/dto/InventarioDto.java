package co.edu.itm.prestamosdeportivos.web.dto;

import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.CODIGO;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.MAXIMO_CARACTERES;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.OBLIGATORIO;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.PATRON_CODIGO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaCategoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** CU-08 a CU-11 y CU-14 (selección de unidades) (BE-01 §10.4). */
public final class InventarioDto {

    private InventarioDto() {
    }

    public record CrearImplementoSolicitud(
            @Schema(example = "BAL-003") @NotBlank(message = OBLIGATORIO) @Size(max = 30, message = MAXIMO_CARACTERES)
            @Pattern(regexp = PATRON_CODIGO, message = CODIGO)
            String codigoInventario,
            @Schema(example = "Balón de baloncesto") @NotBlank(message = OBLIGATORIO)
            @Size(max = 120, message = MAXIMO_CARACTERES)
            String nombre,
            @NotNull(message = OBLIGATORIO) UUID categoriaId,
            @Size(max = 1000, message = MAXIMO_CARACTERES) String descripcion,
            @Schema(description = "DISPONIBLE (por defecto) o EN_MANTENIMIENTO.") EstadoImplemento estado) {
    }

    /** El estado no se modifica aquí: se usa {@link CambioEstadoImplementoSolicitud} (CU-11). */
    public record ActualizarImplementoSolicitud(
            @NotBlank(message = OBLIGATORIO) @Size(max = 30, message = MAXIMO_CARACTERES)
            @Pattern(regexp = PATRON_CODIGO, message = CODIGO) String codigoInventario,
            @NotBlank(message = OBLIGATORIO) @Size(max = 120, message = MAXIMO_CARACTERES) String nombre,
            @NotNull(message = OBLIGATORIO) UUID categoriaId,
            @Size(max = 1000, message = MAXIMO_CARACTERES) String descripcion) {
    }

    public record CambioEstadoImplementoSolicitud(
            @NotNull(message = OBLIGATORIO) EstadoImplemento estado,
            @Schema(example = "Desgaste irreparable") @NotBlank(message = OBLIGATORIO)
            @Size(max = 500, message = MAXIMO_CARACTERES) String motivo) {
    }

    public record ImplementoRespuesta(UUID id, String codigoInventario, String nombre, String descripcion,
            ReferenciaCategoria categoria, EstadoImplemento estado, boolean prestable, OffsetDateTime fechaRegistro) {
    }

    /**
     * Página del inventario más {@code resumen} de conteos sobre todo el
     * inventario: claves {@code total} y cada {@link EstadoImplemento}.
     */
    public record InventarioPaginaRespuesta(List<ImplementoRespuesta> contenido, int pagina, int tamano,
            long totalElementos, int totalPaginas,
            @Schema(example = "{\"total\":100,\"DISPONIBLE\":80,\"PRESTADO\":6,\"EN_MANTENIMIENTO\":10,\"DADO_DE_BAJA\":4}")
            Map<String, Long> resumen) {
    }
}
