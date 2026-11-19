package co.edu.itm.prestamosdeportivos.web.error;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Punto único de traducción de excepciones a {@link ProblemaRespuesta}
 * (BE-01 §4.8 y §4.9, anexo A). Nunca expone mensajes de excepciones técnicas,
 * SQL, nombres de constraints ni trazas: solo textos del catálogo o mensajes de
 * {@link ExcepcionDominio}, que se redactan para la persona usuaria.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);
    private static final String ESTADO_SQL_UNICIDAD = "23505";
    private static final String MENSAJE_FORMATO_INVALIDO = "tiene un formato inválido";

    private final FabricaProblemas fabrica;
    private final AuthenticationTrustResolver resolutorConfianza = new AuthenticationTrustResolverImpl();

    public ManejadorGlobalErrores(FabricaProblemas fabrica) {
        this.fabrica = fabrica;
    }

    // ------------------------------------------------------------------ negocio

    @ExceptionHandler(ExcepcionDominio.class)
    public ResponseEntity<Object> manejarDominio(ExcepcionDominio ex, HttpServletRequest solicitud) {
        HttpStatus estado = EstadoHttpPorTipoError.de(ex.getCodigo().tipo());
        LOG.debug("Error de negocio {} en {} {}", ex.getCodigo(), solicitud.getMethod(), solicitud.getRequestURI());
        return responder(estado, ex.getCodigo(), ex.getMessage(), solicitud.getRequestURI(), ex.getErrores());
    }

    // ---------------------------------------------------------------- seguridad

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> manejarAutenticacion(AuthenticationException ex, HttpServletRequest solicitud) {
        // Sin credenciales → NO_AUTENTICADO; credenciales erróneas → CREDENCIALES_INVALIDAS;
        // token presente pero inválido, expirado o revocado → SESION_INVALIDA.
        CodigoError codigo;
        if (ex instanceof BadCredentialsException) {
            codigo = CodigoError.CREDENCIALES_INVALIDAS;
        } else if (ex instanceof InsufficientAuthenticationException) {
            codigo = CodigoError.NO_AUTENTICADO;
        } else {
            codigo = CodigoError.SESION_INVALIDA;
        }
        return responder(HttpStatus.UNAUTHORIZED, codigo, null, solicitud.getRequestURI(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest solicitud) {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || resolutorConfianza.isAnonymous(autenticacion)) {
            return responder(HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO, null,
                    solicitud.getRequestURI(), List.of());
        }
        LOG.info("Acceso denegado en {} {}", solicitud.getMethod(), solicitud.getRequestURI());
        return responder(HttpStatus.FORBIDDEN, CodigoError.ACCESO_DENEGADO, null, solicitud.getRequestURI(),
                List.of());
    }

    // ---------------------------------------------------------- infraestructura

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> manejarIntegridad(DataIntegrityViolationException ex, HttpServletRequest solicitud) {
        CodigoError codigo = esViolacionDeUnicidad(ex) ? CodigoError.CONFLICTO_UNICIDAD : CodigoError.CONFLICTO_INTEGRIDAD;
        LOG.warn("Violación de integridad ({}) en {} {}", codigo, solicitud.getMethod(), solicitud.getRequestURI());
        return responder(HttpStatus.CONFLICT, codigo, null, solicitud.getRequestURI(), List.of());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<Object> manejarConcurrencia(RuntimeException ex, HttpServletRequest solicitud) {
        LOG.warn("Conflicto de concurrencia en {} {}", solicitud.getMethod(), solicitud.getRequestURI());
        return responder(HttpStatus.CONFLICT, CodigoError.CONFLICTO_CONCURRENCIA, null, solicitud.getRequestURI(),
                List.of());
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<Object> manejarDependenciaNoDisponible(RuntimeException ex, HttpServletRequest solicitud) {
        LOG.error("Dependencia no disponible en {} {}", solicitud.getMethod(), solicitud.getRequestURI(), ex);
        return responder(HttpStatus.SERVICE_UNAVAILABLE, CodigoError.SERVICIO_NO_DISPONIBLE, null,
                solicitud.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> manejarInesperado(Exception ex, HttpServletRequest solicitud) {
        LOG.error("Error no controlado en {} {}", solicitud.getMethod(), solicitud.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO, null,
                solicitud.getRequestURI(), List.of());
    }

    // ------------------------------------------------- excepciones de Spring MVC

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.add(new ErrorCampo(error.getField(), mensajeDeCampo(error)));
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errores.add(new ErrorCampo(error.getObjectName(), error.getDefaultMessage())));
        return responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION_FALLIDA, null, ruta(request), errores);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = new ArrayList<>();
        ex.getParameterValidationResults().forEach(resultado -> resultado.getResolvableErrors().forEach(error ->
                errores.add(new ErrorCampo(resultado.getMethodParameter().getParameterName(),
                        error.getDefaultMessage()))));
        return responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION_FALLIDA, null, ruta(request), errores);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String campo = ex instanceof MethodArgumentTypeMismatchException argumento
                ? argumento.getName()
                : Optional.ofNullable(ex.getPropertyName()).orElse("parametro");
        return responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION_FALLIDA, null, ruta(request),
                List.of(new ErrorCampo(campo, MENSAJE_FORMATO_INVALIDO)));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, CodigoError.VALIDACION_FALLIDA, null, ruta(request),
                List.of(new ErrorCampo(ex.getParameterName(), "es obligatorio")));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, CodigoError.SOLICITUD_INVALIDA,
                "El cuerpo de la solicitud no es un JSON válido o tiene campos con un formato incorrecto "
                        + "(revise fechas ISO-8601 con zona horaria, UUID y valores permitidos).",
                ruta(request), List.of());
    }

    /**
     * Resto de excepciones estándar de Spring MVC (404 de ruta inexistente,
     * 405, 406, 415, etc.): se reemplaza el cuerpo por defecto, en inglés y con
     * detalles internos, por un {@link ProblemaRespuesta} del catálogo.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        CodigoError codigo = codigoParaEstado(statusCode);
        if (statusCode.is5xxServerError()) {
            LOG.error("Error de infraestructura MVC en {}", ruta(request), ex);
        }
        ProblemaRespuesta problema = fabrica.crear(statusCode, codigo, null, ruta(request), List.of());
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }

    // --------------------------------------------------------------- auxiliares

    static CodigoError codigoParaEstado(HttpStatusCode estado) {
        return switch (estado.value()) {
            case 401 -> CodigoError.NO_AUTENTICADO;
            case 403 -> CodigoError.ACCESO_DENEGADO;
            case 404 -> CodigoError.RECURSO_NO_ENCONTRADO;
            case 405 -> CodigoError.METODO_NO_PERMITIDO;
            case 406 -> CodigoError.TIPO_CONTENIDO_NO_ACEPTABLE;
            case 409 -> CodigoError.CONFLICTO_INTEGRIDAD;
            case 415 -> CodigoError.TIPO_CONTENIDO_NO_SOPORTADO;
            case 503 -> CodigoError.SERVICIO_NO_DISPONIBLE;
            default -> estado.is5xxServerError() ? CodigoError.ERROR_INTERNO : CodigoError.SOLICITUD_INVALIDA;
        };
    }

    static boolean esViolacionDeUnicidad(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql && ESTADO_SQL_UNICIDAD.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static String mensajeDeCampo(FieldError error) {
        if (error.isBindingFailure() || "typeMismatch".equals(error.getCode())) {
            return MENSAJE_FORMATO_INVALIDO;
        }
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "no es válido";
    }

    private ResponseEntity<Object> responder(HttpStatusCode estado, CodigoError codigo, String detalle, String ruta,
            List<ErrorCampo> errores) {
        return fabrica.respuesta(fabrica.crear(estado, codigo, detalle, ruta, errores));
    }

    private static String ruta(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : null;
    }
}
