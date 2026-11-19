package co.edu.itm.prestamosdeportivos.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Seguridad base de la API (BE-01 §3.2 y §5).
 *
 * <ul>
 *   <li>Deny by default: toda ruta no declarada pública exige autenticación.</li>
 *   <li>Sesión stateless, sin formulario de login ni HTTP Basic y sin CSRF
 *       (el token viajará en la cabecera {@code Authorization}).</li>
 *   <li>Las rutas públicas son las que BE-01 declara: salud, login,
 *       disponibilidad y categorías.</li>
 * </ul>
 *
 * La autenticación JWT se incorpora en el issue de autenticación; mientras
 * tanto, las rutas protegidas responden {@code 401}.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class ConfiguracionSeguridad {

    @Bean
    SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/disponibilidad", "/api/v1/categorias").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * Evita que Spring Boot genere un usuario en memoria con contraseña
     * aleatoria. Se reemplaza por la búsqueda de usuarios en base de datos al
     * implementar la autenticación (CU-01).
     */
    @Bean
    UserDetailsService servicioUsuariosSinCuentas() {
        return nombreUsuario -> {
            throw new UsernameNotFoundException("La autenticación aún no está implementada.");
        };
    }
}
