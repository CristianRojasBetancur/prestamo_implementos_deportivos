package co.edu.itm.prestamosdeportivos.web.dto;

/** Mensajes de Bean Validation en español, comunes a todos los DTO. */
public final class MensajesValidacion {

    public static final String OBLIGATORIO = "es obligatorio";
    public static final String MAXIMO_CARACTERES = "debe tener como máximo {max} caracteres";
    public static final String RANGO_CARACTERES = "debe tener entre {min} y {max} caracteres";
    public static final String CORREO = "debe ser un correo electrónico válido";
    public static final String CODIGO = "solo admite letras, números y guiones";
    public static final String LISTA_NO_VACIA = "debe contener al menos un elemento";
    public static final String MAXIMO_ELEMENTOS = "admite como máximo {max} elementos";

    /** Identificación y código de inventario: letras, números y guiones. */
    public static final String PATRON_CODIGO = "^[A-Za-z0-9-]+$";

    private MensajesValidacion() {
    }
}
