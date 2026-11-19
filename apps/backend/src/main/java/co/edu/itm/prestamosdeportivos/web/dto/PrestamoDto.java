package co.edu.itm.prestamosdeportivos.web.dto;

import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.LISTA_NO_VACIA;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.MAXIMO_ELEMENTOS;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.OBLIGATORIO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaDevolucion;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaImplemento;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaIncidencia;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CU-14 y CU-15 (BE-01 §10.6). */
public final class PrestamoDto {

    private PrestamoDto() {
    }

    /** Las fechas las calcula el servidor (BE-01 D-07); el cliente no las envía. */
    public record CrearPrestamoSolicitud(
            @NotNull(message = OBLIGATORIO) UUID usuarioId,
            @Schema(description = "De 1 a 4 unidades físicas, sin repetidos.")
            @NotEmpty(message = LISTA_NO_VACIA) @Size(max = 4, message = MAXIMO_ELEMENTOS)
            List<@NotNull(message = OBLIGATORIO) UUID> implementoIds) {
    }

    public record DetallePrestamoRespuesta(UUID id, ReferenciaImplemento implemento, EstadoDetallePrestamo estado) {
    }

    /** Elemento de listado (sin detalles). */
    public record PrestamoResumenRespuesta(UUID id, String codigo, EstadoPrestamo estado, ReferenciaUsuario usuario,
            OffsetDateTime fechaPrestamo, OffsetDateTime fechaLimiteDevolucion, boolean vencido,
            int cantidadImplementos) {
    }

    /** Detalle completo. {@code devoluciones} e {@code incidencias} solo en GET /prestamos/{id}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PrestamoRespuesta(UUID id, String codigo, EstadoPrestamo estado, ReferenciaUsuario usuario,
            ReferenciaUsuario prestador, OffsetDateTime fechaPrestamo, OffsetDateTime fechaLimiteDevolucion,
            @JsonInclude(JsonInclude.Include.ALWAYS) OffsetDateTime fechaFinalizacion, boolean vencido,
            int unidadesPendientes, List<DetallePrestamoRespuesta> detalles, List<ReferenciaDevolucion> devoluciones,
            List<ReferenciaIncidencia> incidencias) {
    }
}
