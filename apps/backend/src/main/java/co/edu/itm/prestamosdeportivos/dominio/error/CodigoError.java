package co.edu.itm.prestamosdeportivos.dominio.error;

import java.util.Locale;

/**
 * Catálogo estable de códigos de error (BE-01 §4.9 y anexo A). El frontend
 * decide su comportamiento con {@link #name()}; nunca se renombra un código
 * existente.
 */
public enum CodigoError {

    // 400
    SOLICITUD_INVALIDA(TipoError.SOLICITUD_INVALIDA, "Solicitud inválida",
            "La solicitud no tiene un formato válido."),
    VALIDACION_FALLIDA(TipoError.SOLICITUD_INVALIDA, "Datos inválidos",
            "Uno o más campos no son válidos."),
    // 401
    NO_AUTENTICADO(TipoError.NO_AUTENTICADO, "Autenticación requerida",
            "Debe iniciar sesión para acceder a este recurso."),
    CREDENCIALES_INVALIDAS(TipoError.NO_AUTENTICADO, "Credenciales inválidas",
            "El usuario o la contraseña no son válidos."),
    SESION_INVALIDA(TipoError.NO_AUTENTICADO, "Sesión inválida",
            "La sesión expiró o ya no es válida. Inicie sesión de nuevo."),
    // 403
    ACCESO_DENEGADO(TipoError.ACCESO_DENEGADO, "Acceso denegado",
            "Su rol no tiene permiso para realizar esta operación."),
    // 404
    RECURSO_NO_ENCONTRADO(TipoError.NO_ENCONTRADO, "Recurso no encontrado",
            "El recurso solicitado no existe."),
    // 405, 406, 415
    METODO_NO_PERMITIDO(TipoError.METODO_NO_PERMITIDO, "Método no permitido",
            "La ruta no admite el método HTTP utilizado."),
    TIPO_CONTENIDO_NO_ACEPTABLE(TipoError.NO_ACEPTABLE, "Formato de respuesta no disponible",
            "La API solo responde en formato JSON."),
    TIPO_CONTENIDO_NO_SOPORTADO(TipoError.MEDIO_NO_SOPORTADO, "Tipo de contenido no soportado",
            "El cuerpo de la solicitud debe enviarse como application/json."),
    // 409
    CONFLICTO_UNICIDAD(TipoError.CONFLICTO, "Registro duplicado",
            "Ya existe un registro con esos datos."),
    CONFLICTO_INTEGRIDAD(TipoError.CONFLICTO, "Conflicto de integridad",
            "La operación entra en conflicto con información relacionada."),
    CONFLICTO_CONCURRENCIA(TipoError.CONFLICTO, "Conflicto de concurrencia",
            "Otro usuario modificó la información al mismo tiempo. Intente de nuevo."),
    IMPLEMENTO_NO_DISPONIBLE(TipoError.CONFLICTO, "Implemento no disponible",
            "Uno o más implementos no están disponibles para préstamo."),
    TRANSICION_INVALIDA(TipoError.CONFLICTO, "Transición de estado no permitida",
            "El recurso no admite ese cambio de estado."),
    PRESTAMO_FINALIZADO(TipoError.CONFLICTO, "Préstamo finalizado",
            "El préstamo ya está finalizado y no admite más operaciones."),
    UNIDAD_YA_RESUELTA(TipoError.CONFLICTO, "Unidad ya resuelta",
            "La unidad ya fue devuelta o resuelta mediante una incidencia."),
    INCIDENCIA_YA_RESUELTA(TipoError.CONFLICTO, "Incidencia ya resuelta",
            "La incidencia ya fue resuelta y no admite otra resolución."),
    // 422
    LIMITE_IMPLEMENTOS_EXCEDIDO(TipoError.REGLA_NEGOCIO, "Límite de implementos excedido",
            "El usuario superaría el máximo de 4 implementos activos."),
    USUARIO_NO_HABILITADO(TipoError.REGLA_NEGOCIO, "Usuario no habilitado",
            "El usuario no está habilitado para recibir préstamos."),
    USUARIO_SANCIONADO(TipoError.REGLA_NEGOCIO, "Usuario sancionado",
            "El usuario tiene una suspensión vigente."),
    USUARIO_CON_PRESTAMOS_VENCIDOS(TipoError.REGLA_NEGOCIO, "Usuario con préstamos vencidos",
            "El usuario tiene préstamos vencidos pendientes de devolución."),
    INCIDENCIA_REQUERIDA(TipoError.REGLA_NEGOCIO, "Incidencia requerida",
            "Una unidad recibida con novedad requiere registrar una incidencia."),
    IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO(TipoError.REGLA_NEGOCIO, "Implemento ajeno al préstamo",
            "El implemento no pertenece al préstamo indicado."),
    TIPO_INCIDENCIA_NO_PERMITIDO(TipoError.REGLA_NEGOCIO, "Tipo de incidencia no permitido",
            "El tipo de incidencia no está permitido en esta operación."),
    JUSTIFICACION_REQUERIDA(TipoError.REGLA_NEGOCIO, "Justificación requerida",
            "Debe registrar la justificación del retraso."),
    CATEGORIA_INACTIVA(TipoError.REGLA_NEGOCIO, "Categoría inactiva",
            "La categoría está inactiva y no admite implementos nuevos."),
    // 500, 503
    ERROR_INTERNO(TipoError.INTERNO, "Error interno",
            "Ocurrió un error inesperado. Si persiste, informe el identificador de correlación."),
    SERVICIO_NO_DISPONIBLE(TipoError.NO_DISPONIBLE, "Servicio no disponible",
            "El servicio no está disponible en este momento. Intente más tarde.");

    private final TipoError tipo;
    private final String titulo;
    private final String detallePorDefecto;

    CodigoError(TipoError tipo, String titulo, String detallePorDefecto) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.detallePorDefecto = detallePorDefecto;
    }

    public TipoError tipo() {
        return tipo;
    }

    public String titulo() {
        return titulo;
    }

    public String detallePorDefecto() {
        return detallePorDefecto;
    }

    /** Segmento usado en el campo {@code type}: {@code IMPLEMENTO_NO_DISPONIBLE} → {@code implemento-no-disponible}. */
    public String slug() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
