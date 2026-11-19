package co.edu.itm.prestamosdeportivos.configuracion;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Asigna a cada solicitud un identificador de correlación (anexo A de BE-01).
 * Reutiliza la cabecera {@code X-Correlation-Id} entrante si es segura o genera
 * un UUID; lo publica en el MDC de logs, en la cabecera de respuesta y en el
 * cuerpo de los errores. Se ejecuta antes que Spring Security para que también
 * los 401/403 lo incluyan.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroCorrelacion extends OncePerRequestFilter {

    public static final String CABECERA = "X-Correlation-Id";
    public static final String CLAVE_MDC = "correlacionId";

    /** Evita inyección en logs o cabeceras: solo caracteres seguros y longitud acotada. */
    private static final Pattern FORMATO_SEGURO = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String entrante = request.getHeader(CABECERA);
        String correlacion = entrante != null && FORMATO_SEGURO.matcher(entrante).matches()
                ? entrante
                : UUID.randomUUID().toString();
        MDC.put(CLAVE_MDC, correlacion);
        response.setHeader(CABECERA, correlacion);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_MDC);
        }
    }
}
