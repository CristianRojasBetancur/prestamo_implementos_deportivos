package co.edu.itm.prestamosdeportivos.web.dto;

import java.time.Instant;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contratos REST para BE-07; no exponen entidades ni datos internos. */
public final class GestionInventarioDto {

    private GestionInventarioDto() {
    }

    public record CrearUnidadSolicitud(
            @NotBlank(message = MensajesValidacion.OBLIGATORIO)
            @Size(max = 30, message = MensajesValidacion.MAXIMO_CARACTERES)
            @Pattern(regexp = MensajesValidacion.PATRON_CODIGO, message = MensajesValidacion.CODIGO)
            String codigoInventario,
            @NotBlank(message = MensajesValidacion.OBLIGATORIO)
            @Size(max = 120, message = MensajesValidacion.MAXIMO_CARACTERES) String nombre,
            @Size(max = 1000, message = MensajesValidacion.MAXIMO_CARACTERES) String descripcion,
            @NotNull(message = MensajesValidacion.OBLIGATORIO) UUID categoriaId,
            EstadoImplemento estado) {
    }

    /** Campos nulos se conservan; el estado siempre se modifica por /state. */
    public record ActualizarUnidadSolicitud(
            @Size(max = 30, message = MensajesValidacion.MAXIMO_CARACTERES)
            @Pattern(regexp = MensajesValidacion.PATRON_CODIGO, message = MensajesValidacion.CODIGO) String codigoInventario,
            @Size(max = 120, message = MensajesValidacion.MAXIMO_CARACTERES) String nombre,
            @Size(max = 1000, message = MensajesValidacion.MAXIMO_CARACTERES) String descripcion,
            UUID categoriaId) {
    }

    public record CambioEstadoSolicitud(
            @NotNull(message = MensajesValidacion.OBLIGATORIO) EstadoImplemento estado,
            @NotBlank(message = MensajesValidacion.OBLIGATORIO)
            @Size(max = 500, message = MensajesValidacion.MAXIMO_CARACTERES) String motivo) {
    }

    public record UnidadRespuesta(UUID id, String codigoInventario, String nombre, String descripcion,
            ReferenciaCategoria categoria, EstadoImplemento estado, boolean prestable, Instant creadoEn,
            Instant actualizadoEn) {
    }

    public record CategoriaSolicitud(
            @NotBlank(message = MensajesValidacion.OBLIGATORIO)
            @Size(max = 80, message = MensajesValidacion.MAXIMO_CARACTERES) String nombre,
            @Size(max = 500, message = MensajesValidacion.MAXIMO_CARACTERES) String descripcion,
            Boolean activa) {
    }

    public record CategoriaRespuesta(UUID id, String nombre, String descripcion, boolean activa,
            Instant creadoEn, Instant actualizadoEn) {
    }
}
