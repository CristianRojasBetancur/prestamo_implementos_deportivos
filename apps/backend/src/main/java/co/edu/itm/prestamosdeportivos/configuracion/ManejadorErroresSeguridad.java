package co.edu.itm.prestamosdeportivos.configuracion;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final Clock reloj;
    private final ZoneId zonaHoraria;

    public ManejadorErroresSeguridad(ObjectMapper objectMapper, Clock reloj,
            @Value("${app.zona-horaria}") String zonaHoraria) {
        this.objectMapper = objectMapper;
        this.reloj = reloj;
        this.zonaHoraria = ZoneId.of(zonaHoraria);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        escribir(request, response, HttpStatus.UNAUTHORIZED, "No autenticado",
                "Se requiere autenticación para acceder a este recurso.", "NO_AUTENTICADO");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        escribir(request, response, HttpStatus.FORBIDDEN, "Acceso denegado",
                "El rol autenticado no permite realizar esta operación.", "ACCESO_DENEGADO");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus estado,
            String titulo, String detalle, String codigo) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(estado, detalle);
        problem.setTitle(titulo);
        problem.setType(java.net.URI.create("https://prestamos.itm.edu.co/errores/" + codigo.toLowerCase()));
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("codigo", codigo);
        problem.setProperty("timestamp", OffsetDateTime.ofInstant(reloj.instant(), zonaHoraria));
        problem.setProperty("correlacionId", UUID.randomUUID());
        response.setStatus(estado.value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
