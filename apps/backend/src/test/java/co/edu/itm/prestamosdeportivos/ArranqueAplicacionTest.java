package co.edu.itm.prestamosdeportivos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
class ArranqueAplicacionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock reloj;

    @Test
    @DisplayName("El contexto de la aplicación levanta en el perfil test")
    void contextoLevanta() {
        assertThat(reloj.getZone().getId()).isEqualTo("America/Bogota");
    }

    @Test
    @DisplayName("Flyway aplica las migraciones pendientes sobre una base vacía")
    void flywayAplicaMigraciones() {
        Integer aplicadas = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version = '1'",
                Integer.class);
        assertThat(aplicadas).isEqualTo(1);
    }
}
