package co.edu.itm.prestamosdeportivos.configuracion;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Orígenes del frontend autorizados para CORS (BE-01 §4.10). Se configuran con la
 * variable de entorno {@code APP_CORS_ORIGENES} como lista separada por comas.
 * No se aceptan comodines: cada origen debe declararse de forma explícita.
 */
@Validated
@ConfigurationProperties("app.cors")
public record PropiedadesCors(@NotEmpty List<String> origenesPermitidos) {

    public PropiedadesCors {
        if (origenesPermitidos != null) {
            origenesPermitidos = origenesPermitidos.stream()
                    .map(String::trim)
                    .filter(origen -> !origen.isEmpty())
                    .toList();
            if (origenesPermitidos.stream().anyMatch(origen -> origen.contains("*"))) {
                throw new IllegalArgumentException(
                        "app.cors.origenes-permitidos no admite comodines (*); declare cada origen explícitamente.");
            }
        }
    }
}
