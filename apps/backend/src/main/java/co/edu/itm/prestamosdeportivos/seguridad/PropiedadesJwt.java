package co.edu.itm.prestamosdeportivos.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración del JWT (BE-01 §5). El secreto proviene de
 * {@code APP_JWT_SECRETO} y debe tener al menos 256 bits; la aplicación no
 * arranca con un secreto más corto.
 */
@Validated
@ConfigurationProperties("app.jwt")
public record PropiedadesJwt(
        @NotBlank String secreto,
        @NotNull Duration expiracion,
        @NotBlank String emisor) {

    static final int BYTES_MINIMOS_SECRETO = 32;
    static final String VALOR_DE_EJEMPLO = "reemplace-por-";

    public PropiedadesJwt {
        if (secreto != null && !secreto.isBlank()
                && secreto.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalArgumentException(
                    "app.jwt.secreto (APP_JWT_SECRETO) debe tener al menos 32 bytes (256 bits).");
        }
        if (secreto != null && secreto.startsWith(VALOR_DE_EJEMPLO)) {
            throw new IllegalArgumentException(
                    "APP_JWT_SECRETO tiene el valor de ejemplo de .env.example; genere un secreto propio.");
        }
        if (expiracion != null && (expiracion.isNegative() || expiracion.isZero())) {
            throw new IllegalArgumentException("app.jwt.expiracion debe ser positiva.");
        }
    }

    /** Nunca exponer el secreto en logs ni en el endpoint de configuración. */
    @Override
    public String toString() {
        return "PropiedadesJwt[secreto=***, expiracion=" + expiracion + ", emisor=" + emisor + "]";
    }
}
