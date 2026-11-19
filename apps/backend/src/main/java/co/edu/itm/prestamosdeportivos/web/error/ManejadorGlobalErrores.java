package co.edu.itm.prestamosdeportivos.web.error;

import java.time.Clock;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ManejadorGlobalErrores {

    private final Clock reloj;
    private final ZoneId zonaHoraria;

    public ManejadorGlobalErrores(Clock reloj, @Value("${app.zona-horaria}") String zonaHoraria) {
        this.reloj = reloj;
        this.zonaHoraria = ZoneId.of(zonaHoraria);
    }

    @ExceptionHandler(ExcepcionApi.class)
    ResponseEntity<ProblemDetail> manejarApi(ExcepcionApi ex, HttpServletRequest request) {
        ProblemDetail problem = crear(ex.getEstadoHttp(), ex.getTitulo(), ex.getMessage(), ex.getCodigo(), request);
        if (!ex.getErrores().isEmpty()) {
            problem.setProperty("errores", ex.getErrores());
        }
        return ResponseEntity.status(ex.getEstadoHttp()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> manejarValidacion(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorCampo(error.getField(), error.getDefaultMessage() == null
                        ? "El valor no es válido." : error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = crear(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Uno o más campos no cumplen las validaciones.", "VALIDACION_FALLIDA", request);
        problem.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ProblemDetail> manejarSolicitudInvalida(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = crear(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "La solicitud no tiene el formato esperado.", "SOLICITUD_INVALIDA", request);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> manejarNoEncontrado(NoResourceFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = crear(HttpStatus.NOT_FOUND, "Recurso no encontrado",
                "No existe el recurso solicitado.", "RECURSO_NO_ENCONTRADO", request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    private ProblemDetail crear(HttpStatus estado, String titulo, String detalle, String codigo,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(estado, detalle);
        problem.setTitle(titulo);
        problem.setType(java.net.URI.create("https://prestamos.itm.edu.co/errores/" + codigo.toLowerCase()));
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("codigo", codigo);
        problem.setProperty("timestamp", OffsetDateTime.ofInstant(reloj.instant(), zonaHoraria));
        problem.setProperty("correlacionId", UUID.randomUUID());
        return problem;
    }
}
