package co.edu.itm.prestamosdeportivos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL efímero para el perfil {@code test}. Usa la misma imagen que
 * {@code compose.yaml} y Spring Boot configura el datasource automáticamente
 * mediante {@link ServiceConnection}. Requiere Docker en ejecución.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedoresPrueba {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
