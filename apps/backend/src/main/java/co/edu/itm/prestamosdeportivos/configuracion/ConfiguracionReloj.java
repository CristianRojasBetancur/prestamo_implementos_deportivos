package co.edu.itm.prestamosdeportivos.configuracion;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Única fuente de "ahora" de la aplicación (BE-01 §4.4 y §12). Las reglas de
 * negocio que dependen del tiempo (RN-02, RN-03) reciben este {@link Clock}
 * inyectado, lo que permite pruebas deterministas con {@code Clock.fixed}.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionReloj {

    @Bean
    Clock reloj(@Value("${app.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
