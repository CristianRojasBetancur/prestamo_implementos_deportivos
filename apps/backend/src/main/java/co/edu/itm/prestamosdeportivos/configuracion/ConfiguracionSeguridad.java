package co.edu.itm.prestamosdeportivos.configuracion;

import co.edu.itm.prestamosdeportivos.seguridad.ConversorJwtAUsuario;
import co.edu.itm.prestamosdeportivos.web.error.DelegacionErroresSeguridad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API (RF-01, RF-02, RNF-03; BE-01 §3.2 y §5).
 *
 * <ul>
 *   <li><b>Autenticación:</b> JWT Bearer validado por el resource server de
 *       Spring Security; cada token se verifica además contra la base de datos
 *       ({@link ConversorJwtAUsuario}).</li>
 *   <li><b>Autorización:</b> matriz por rol en las rutas (esta clase) como
 *       primera barrera y {@code @PreAuthorize} en los controladores. Deny by
 *       default: lo que no se declara público exige autenticación.</li>
 *   <li><b>Sin sesión de servidor ni CSRF:</b> la API es stateless y el token
 *       viaja en la cabecera {@code Authorization}, que un sitio de terceros no
 *       puede adjuntar automáticamente (no hay cookies de sesión), por lo que el
 *       ataque CSRF no aplica.</li>
 *   <li>Los 401/403 usan el formato común de errores (anexo A de BE-01).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    static final String ADMIN = "ADMIN";
    static final String PRESTADOR = "PRESTADOR";

    @Bean
    SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http, DelegacionErroresSeguridad erroresSeguridad,
            JwtDecoder decodificadorJwt, ConversorJwtAUsuario conversorJwt) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(servidor -> servidor
                        .authenticationEntryPoint(erroresSeguridad)
                        .accessDeniedHandler(erroresSeguridad)
                        .jwt(jwt -> jwt.decoder(decodificadorJwt).jwtAuthenticationConverter(conversorJwt)))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(erroresSeguridad)
                        .accessDeniedHandler(erroresSeguridad))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        // Público: salud, errores, documentación, login y catálogo (BE-01 §3.2, D-03).
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html",
                                "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/disponibilidad", "/api/v1/categorias").permitAll()

                        // Sesión propia: cualquier usuario con acceso.
                        .requestMatchers("/api/v1/auth/**").hasAnyRole(ADMIN, PRESTADOR)

                        // Administración: solo ADMIN.
                        .requestMatchers("/api/v1/categorias/**", "/api/v1/categorias").hasRole(ADMIN)
                        .requestMatchers("/api/v1/inventory/units", "/api/v1/inventory/units/**").hasRole(ADMIN)
                        .requestMatchers("/api/v1/prestadores", "/api/v1/prestadores/**").hasRole(ADMIN)
                        // La selección de unidades para prestar es operación del PRESTADOR (antes que /implementos/**).
                        .requestMatchers(HttpMethod.GET, "/api/v1/implementos/prestables").hasRole(PRESTADOR)
                        .requestMatchers("/api/v1/implementos", "/api/v1/implementos/**").hasRole(ADMIN)

                        // Operación: solo PRESTADOR.
                        .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasRole(PRESTADOR)
                        .requestMatchers("/api/v1/prestamos", "/api/v1/prestamos/**").hasRole(PRESTADOR)
                        .requestMatchers("/api/v1/devoluciones", "/api/v1/devoluciones/**").hasRole(PRESTADOR)
                        .requestMatchers("/api/v1/incidencias", "/api/v1/incidencias/**").hasRole(PRESTADOR)

                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * La API no autentica con usuario y contraseña de Spring Security (el login
     * propio emite un JWT). Este bean evita que Spring Boot genere un usuario en
     * memoria con contraseña aleatoria.
     */
    @Bean
    UserDetailsService servicioUsuariosSinCuentas() {
        return nombreUsuario -> {
            throw new UsernameNotFoundException("La autenticación se realiza con JWT.");
        };
    }
}
