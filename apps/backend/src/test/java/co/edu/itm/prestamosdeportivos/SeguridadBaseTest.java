package co.edu.itm.prestamosdeportivos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Verifica sobre HTTP real el endpoint de salud, la política deny-by-default y
 * CORS (BE-01 §3.2, §4.10 y criterio 1 de §16).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@ExtendWith(OutputCaptureExtension.class)
class SeguridadBaseTest {

    private static final String ORIGEN_FRONTEND = "http://localhost:5173";

    private final HttpClient cliente = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private HikariDataSource dataSource;

    @Test
    @DisplayName("El healthcheck es público y responde UP con la base de datos disponible")
    void saludPublicaYArriba() throws Exception {
        HttpResponse<String> respuesta = enviar(HttpRequest.newBuilder(uri("/actuator/health")).GET().build());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Las sondas distinguen liveness de readiness y las métricas no son públicas")
    void sondasYMetricas() throws Exception {
        HttpResponse<String> liveness = enviar(HttpRequest.newBuilder(uri("/actuator/health/liveness")).GET().build());
        HttpResponse<String> readiness = enviar(HttpRequest.newBuilder(uri("/actuator/health/readiness")).GET().build());
        HttpResponse<String> metricas = enviar(HttpRequest.newBuilder(uri("/actuator/metrics")).GET().build());

        assertThat(liveness.statusCode()).isEqualTo(200);
        assertThat(liveness.body()).contains("\"status\":\"UP\"");
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(readiness.body()).contains("\"status\":\"UP\"");
        assertThat(metricas.statusCode()).isEqualTo(401);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("Readiness responde no disponible cuando PostgreSQL no está disponible")
    void readinessConBaseNoDisponible() throws Exception {
        dataSource.close();

        HttpResponse<String> respuesta = enviar(HttpRequest.newBuilder(uri("/actuator/health/readiness")).GET().build());

        assertThat(respuesta.statusCode()).isEqualTo(503);
        assertThat(respuesta.body()).contains("\"status\":\"DOWN\"");
    }

    @Test
    @DisplayName("Los logs de una solicitud HTTP no contienen secretos ni cuerpos de solicitud")
    void logsSinDatosSensibles(CapturedOutput salida) throws Exception {
        enviar(HttpRequest.newBuilder(uri("/api/v1/prestamos")).GET().build());

        assertThat(salida.getOut()).doesNotContain("password", "contrasena", "token", "hash", "Authorization");
    }

    @Test
    @DisplayName("Una ruta no declarada pública responde 401 sin token, con el formato de error común")
    void rutaProtegidaSinToken() throws Exception {
        HttpResponse<String> respuesta = enviar(HttpRequest.newBuilder(uri("/api/v1/prestamos")).GET().build());

        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValueSatisfying(
                tipo -> assertThat(tipo).startsWith("application/problem+json"));
        assertThat(respuesta.body()).contains("\"codigo\":\"NO_AUTENTICADO\"");
        assertThat(respuesta.headers().firstValue("X-Correlation-Id")).isPresent();
        assertThat(respuesta.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(respuesta.headers().firstValue("X-Frame-Options")).contains("DENY");
        assertThat(respuesta.headers().firstValue("Referrer-Policy")).contains("strict-origin-when-cross-origin");
    }

    @Test
    @DisplayName("Un identificador de correlación seguro se conserva y una búsqueda excesiva se rechaza")
    void correlacionYLimiteDeBusqueda() throws Exception {
        String correlacion = "prueba-correlation-01";
        HttpRequest solicitud = HttpRequest.newBuilder(uri("/api/v1/disponibilidad?q=" + "x".repeat(81)))
                .header("X-Correlation-Id", correlacion).GET().build();
        HttpResponse<String> respuesta = enviar(solicitud);

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(respuesta.headers().firstValue("X-Correlation-Id")).contains(correlacion);
        assertThat(respuesta.body()).contains("VALIDACION_FALLIDA");
    }

    @Test
    @DisplayName("El documento OpenAPI es público e incluye seguridad Bearer y el formato de error")
    void documentoOpenApi() throws Exception {
        HttpResponse<String> respuesta = enviar(HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("bearerAuth", "ProblemaRespuesta", "correlacionId",
                "/api/v1/usuarios", "/api/v1/prestamos");
        assertThat(respuesta.body()).doesNotContain("/api/v1/loans", "/api/v1/users/{userId}/loan-situation");
    }

    @Test
    @DisplayName("CORS permite el origen configurado del frontend")
    void corsPermiteOrigenConfigurado() throws Exception {
        HttpResponse<String> respuesta = enviar(preflight(ORIGEN_FRONTEND));

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin")).contains(ORIGEN_FRONTEND);
    }

    @Test
    @DisplayName("CORS rechaza un origen no configurado")
    void corsRechazaOrigenNoConfigurado() throws Exception {
        HttpResponse<String> respuesta = enviar(preflight("http://sitio-no-autorizado.example"));

        assertThat(respuesta.statusCode()).isEqualTo(403);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    private HttpRequest preflight(String origen) {
        return HttpRequest.newBuilder(uri("/api/v1/disponibilidad"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origen)
                .header("Access-Control-Request-Method", "GET")
                .build();
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }

    private HttpResponse<String> enviar(HttpRequest solicitud) throws IOException, InterruptedException {
        return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());
    }
}
