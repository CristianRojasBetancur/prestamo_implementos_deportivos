package co.edu.itm.prestamosdeportivos.configuracion;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Política CORS de la API (BE-01 §4.10): solo los orígenes configurados, los
 * métodos del contrato y las cabeceras {@code Authorization} y
 * {@code Content-Type}. Spring Security la aplica mediante {@code cors()}.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionCors {

    @Bean
    CorsConfigurationSource corsConfigurationSource(PropiedadesCors propiedades) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.origenesPermitidos());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setExposedHeaders(List.of("Location"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
