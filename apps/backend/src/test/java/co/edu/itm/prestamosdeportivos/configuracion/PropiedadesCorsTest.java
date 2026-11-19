package co.edu.itm.prestamosdeportivos.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropiedadesCorsTest {

    @Test
    @DisplayName("Rechaza comodines en los orígenes permitidos")
    void rechazaComodines() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PropiedadesCors(List.of("https://app.itm.edu.co", "*")));
    }

    @Test
    @DisplayName("Normaliza espacios y descarta entradas vacías")
    void normalizaOrigenes() {
        PropiedadesCors propiedades = new PropiedadesCors(List.of(" http://localhost:5173 ", ""));

        assertThat(propiedades.origenesPermitidos()).containsExactly("http://localhost:5173");
    }
}
