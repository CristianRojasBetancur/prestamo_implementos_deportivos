package co.edu.itm.prestamosdeportivos.dominio;

/**
 * Estado del préstamo según la máquina de estados del Entregable 3 (BE-01 §6.3).
 * Se persiste como texto con el nombre exacto de la constante
 * (ver docs/agent-context/modelo-datos.md). No renombrar constantes sin una
 * migración que actualice los datos y el CHECK correspondiente.
 */
public enum EstadoPrestamo {
    VIGENTE,
    DEVOLUCION_PARCIAL,
    VENCIDO,
    FINALIZADO;
}
