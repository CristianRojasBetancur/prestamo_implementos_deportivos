package co.edu.itm.prestamosdeportivos.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Los errores 401/403 que produce la cadena de filtros de Spring Security
 * (antes de llegar a un controlador) se delegan al mismo
 * {@link ManejadorGlobalErrores}, para que tengan el formato de cualquier otro
 * error de la API.
 */
@Component
public class DelegacionErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolutor;

    public DelegacionErroresSeguridad(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolutor) {
        this.resolutor = resolutor;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) {
        resolutor.resolveException(request, response, null, authException);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) {
        resolutor.resolveException(request, response, null, accessDeniedException);
    }
}
