package co.edu.itm.prestamosdeportivos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void extenderSuspensionConservaLaFechaMasLejana() {
        Usuario usuario = Usuario.solicitante("123", "Ana", "Rojas");
        Instant primera = Instant.parse("2026-11-21T00:00:00Z");
        Instant masLejana = Instant.parse("2026-11-25T00:00:00Z");

        usuario.extenderSuspensionHasta(masLejana);
        usuario.extenderSuspensionHasta(primera);

        assertThat(usuario.getSuspendidoHasta()).isEqualTo(masLejana);
    }
}
