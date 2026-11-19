package co.edu.itm.prestamosdeportivos.dominio.error;

/**
 * Clasificación de un error independiente del protocolo. La capa web traduce
 * cada tipo a un estado HTTP (ver {@code EstadoHttpPorTipoError}).
 */
public enum TipoError {
    /** Solicitud mal formada o con datos sintácticamente inválidos (400). */
    SOLICITUD_INVALIDA,
    /** Falta autenticación o es inválida (401). */
    NO_AUTENTICADO,
    /** Autenticado, pero el rol no permite la operación (403). */
    ACCESO_DENEGADO,
    /** El recurso solicitado o referenciado no existe (404). */
    NO_ENCONTRADO,
    /** Método HTTP no soportado por la ruta (405). */
    METODO_NO_PERMITIDO,
    /** El formato de respuesta pedido en Accept no se puede producir (406). */
    NO_ACEPTABLE,
    /** Conflicto con el estado actual de otro recurso (409). */
    CONFLICTO,
    /** Content-Type no soportado (415). */
    MEDIO_NO_SOPORTADO,
    /** Regla de negocio sobre el solicitante o la semántica de la operación (422). */
    REGLA_NEGOCIO,
    /** Error no previsto (500). */
    INTERNO,
    /** Una dependencia (por ejemplo, la base de datos) no está disponible (503). */
    NO_DISPONIBLE
}
