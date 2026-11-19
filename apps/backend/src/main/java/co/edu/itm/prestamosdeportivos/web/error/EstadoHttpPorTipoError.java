package co.edu.itm.prestamosdeportivos.web.error;

import co.edu.itm.prestamosdeportivos.dominio.error.TipoError;
import org.springframework.http.HttpStatus;

/** Traducción única de {@link TipoError} a estado HTTP (BE-01 §4.9). */
public final class EstadoHttpPorTipoError {

    private EstadoHttpPorTipoError() {
    }

    public static HttpStatus de(TipoError tipo) {
        return switch (tipo) {
            case SOLICITUD_INVALIDA -> HttpStatus.BAD_REQUEST;
            case NO_AUTENTICADO -> HttpStatus.UNAUTHORIZED;
            case ACCESO_DENEGADO -> HttpStatus.FORBIDDEN;
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case METODO_NO_PERMITIDO -> HttpStatus.METHOD_NOT_ALLOWED;
            case NO_ACEPTABLE -> HttpStatus.NOT_ACCEPTABLE;
            case CONFLICTO -> HttpStatus.CONFLICT;
            case MEDIO_NO_SOPORTADO -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case REGLA_NEGOCIO -> HttpStatus.UNPROCESSABLE_CONTENT;
            case INTERNO -> HttpStatus.INTERNAL_SERVER_ERROR;
            case NO_DISPONIBLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
