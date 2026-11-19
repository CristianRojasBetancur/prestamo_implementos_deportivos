package co.edu.itm.prestamosdeportivos.dominio;

/**
 * Indica si la devolución deja unidades pendientes en el préstamo.
 * Se persiste como texto con el nombre exacto de la constante
 * (ver docs/agent-context/modelo-datos.md). No renombrar constantes sin una
 * migración que actualice los datos y el CHECK correspondiente.
 */
public enum AlcanceDevolucion {
    PARCIAL,
    COMPLETA;
}
