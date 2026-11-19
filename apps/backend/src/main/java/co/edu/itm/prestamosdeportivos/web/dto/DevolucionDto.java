package co.edu.itm.prestamosdeportivos.web.dto;

import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.LISTA_NO_VACIA;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.MAXIMO_CARACTERES;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.OBLIGATORIO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.AlcanceDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaImplemento;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaIncidencia;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaPrestamo;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CU-16 y CU-17 (BE-01 §10.7). */
public final class DevolucionDto {

    private DevolucionDto() {
    }

    /** Incidencia que acompaña a un ítem CON_NOVEDAD (BE-01 D-12). */
    public record IncidenciaEnDevolucionSolicitud(
            @Schema(description = "DANO u OTRA; PERDIDA y ROBO no aplican a una unidad devuelta.")
            @NotNull(message = OBLIGATORIO) TipoIncidencia tipo,
            @Schema(example = "Peto con costura lateral rota.")
            @NotBlank(message = OBLIGATORIO) @Size(max = 1000, message = MAXIMO_CARACTERES) String descripcion) {
    }

    public record ItemDevolucionSolicitud(
            @NotNull(message = OBLIGATORIO) UUID implementoId,
            @NotNull(message = OBLIGATORIO) CondicionDevolucion condicion,
            @Size(max = 1000, message = MAXIMO_CARACTERES) String observaciones,
            @Schema(description = "Obligatoria si la condición es CON_NOVEDAD.") @Valid
            IncidenciaEnDevolucionSolicitud incidencia) {
    }

    public record RegistrarDevolucionSolicitud(
            @NotEmpty(message = LISTA_NO_VACIA) List<@Valid @NotNull(message = OBLIGATORIO) ItemDevolucionSolicitud> items,
            @Schema(description = "Solo aplica si la devolución es tardía. Por defecto false.") Boolean retrasoJustificado,
            @Size(max = 1000, message = MAXIMO_CARACTERES) String justificacion,
            @Size(max = 1000, message = MAXIMO_CARACTERES) String observaciones) {
    }

    public record ItemDevolucionRespuesta(ReferenciaImplemento implemento, CondicionDevolucion condicion,
            EstadoImplemento estadoImplementoResultante, ReferenciaIncidencia incidencia) {
    }

    public record SancionAplicadaRespuesta(OffsetDateTime fechaInicio, OffsetDateTime fechaFin, long minutosRetraso) {
    }

    public record DevolucionRespuesta(UUID id, String codigo, ReferenciaPrestamo prestamo, ReferenciaUsuario usuario,
            OffsetDateTime fechaDevolucion, AlcanceDevolucion alcance, boolean retrasoJustificado,
            List<ItemDevolucionRespuesta> items, SancionAplicadaRespuesta sancion) {
    }

    public record DevolucionResumenRespuesta(UUID id, String codigo, ReferenciaPrestamo prestamo,
            ReferenciaUsuario usuario, OffsetDateTime fechaDevolucion, AlcanceDevolucion alcance,
            int cantidadImplementos, boolean conNovedad) {
    }
}
