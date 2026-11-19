package co.edu.itm.prestamosdeportivos.dominio;

/**
 * Condición en la que se recibe una unidad (BE-01 D-11).
 * Se persiste como texto con el nombre exacto de la constante
 * (ver docs/agent-context/modelo-datos.md). No renombrar constantes sin una
 * migración que actualice los datos y el CHECK correspondiente.
 */
public enum CondicionDevolucion {
    BUENO,
    CON_NOVEDAD;
}
