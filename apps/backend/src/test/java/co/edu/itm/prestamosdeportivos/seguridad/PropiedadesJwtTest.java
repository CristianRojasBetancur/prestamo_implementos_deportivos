package co.edu.itm.prestamosdeportivos.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropiedadesJwtTest {

    @Test
    @DisplayName("Rechaza secretos de menos de 256 bits")
    void secretoCorto() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PropiedadesJwt("corto", Duration.ofMinutes(60), "emisor"));
    }

    @Test
    @DisplayName("Rechaza el valor de ejemplo de .env.example")
    void secretoDeEjemplo() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PropiedadesJwt(
                "reemplace-por-un-secreto-aleatorio-de-al-menos-32-bytes", Duration.ofMinutes(60), "emisor"));
    }

    @Test
    @DisplayName("No expone el secreto en toString")
    void ocultaSecreto() {
        var propiedades = new PropiedadesJwt("secreto-de-prueba-suficientemente-largo-123", Duration.ofMinutes(60), "e");

        assertThat(propiedades.toString()).doesNotContain("secreto-de-prueba");
    }
}
