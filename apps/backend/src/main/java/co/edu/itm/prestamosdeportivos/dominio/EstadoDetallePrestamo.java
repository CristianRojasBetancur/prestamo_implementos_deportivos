package co.edu.itm.prestamosdeportivos.dominio;

/**
 * Estado de resolución de una unidad dentro de un préstamo (BE-01 §6.4).
 * Se persiste como texto con el nombre exacto de la constante
 * (ver docs/agent-context/modelo-datos.md). No renombrar constantes sin una
 * migración que actualice los datos y el CHECK correspondiente.
 */
public enum EstadoDetallePrestamo {
    PENDIENTE_DEVOLUCION,
    DEVUELTO,
    RESUELTO_POR_INCIDENCIA;
}
