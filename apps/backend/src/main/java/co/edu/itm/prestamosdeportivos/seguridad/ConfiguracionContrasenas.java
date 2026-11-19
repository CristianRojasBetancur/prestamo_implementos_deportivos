package co.edu.itm.prestamosdeportivos.seguridad;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hash de contraseñas con BCrypt (algoritmo adaptativo, factor 12). Se usa
 * {@link DelegatingPasswordEncoder}: los hashes se guardan con el prefijo
 * {@code {bcrypt}} para poder migrar a otro algoritmo (por ejemplo Argon2) sin
 * invalidar los existentes.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionContrasenas {

    static final int FACTOR_COSTO_BCRYPT = 12;

    @Bean
    PasswordEncoder codificadorContrasenas() {
        String porDefecto = "bcrypt";
        return new DelegatingPasswordEncoder(porDefecto,
                Map.of(porDefecto, new BCryptPasswordEncoder(FACTOR_COSTO_BCRYPT)));
    }
}
