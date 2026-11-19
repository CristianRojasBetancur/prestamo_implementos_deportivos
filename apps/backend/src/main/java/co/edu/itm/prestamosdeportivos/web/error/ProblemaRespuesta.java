package co.edu.itm.prestamosdeportivos.web.error;

import java.util.List;

import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cuerpo de toda respuesta de error ({@code application/problem+json}). Sigue
 * RFC 9457 y lo extiende con {@code codigo}, {@code timestamp},
 * {@code correlacionId} y {@code errores} (BE-01 §4.8, anexo A).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ProblemaRespuesta", description = "Formato estándar de error de la API (RFC 9457 extendido).")
public record ProblemaRespuesta(
        @Schema(description = "URI que identifica el tipo de error.",
                example = "https://prestamos.itm.edu.co/errores/implemento-no-disponible")
        String type,
        @Schema(description = "Resumen corto del tipo de error.", example = "Implemento no disponible")
        String title,
        @Schema(description = "Código de estado HTTP.", example = "409")
        int status,
        @Schema(description = "Mensaje en español para la persona usuaria.",
                example = "El implemento BAL-001 no está disponible para préstamo.")
        String detail,
        @Schema(description = "Ruta de la solicitud que produjo el error.", example = "/api/v1/prestamos")
        String instance,
        @Schema(description = "Código estable del catálogo; el frontend decide con este valor.",
                example = "IMPLEMENTO_NO_DISPONIBLE")
        String codigo,
        @Schema(description = "Instante del error en ISO-8601 con desplazamiento.",
                example = "2026-08-31T18:00:00-05:00")
        String timestamp,
        @Schema(description = "Identificador de correlación (cabecera X-Correlation-Id).",
                example = "3f1c2a4e-9b8d-4c2a-9e51-0a7d3f2b1c11")
        String correlacionId,
        @Schema(description = "Errores por campo; solo cuando aplica.")
        List<ErrorCampo> errores) {
}
