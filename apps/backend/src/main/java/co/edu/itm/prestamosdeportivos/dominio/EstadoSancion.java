package co.edu.itm.prestamosdeportivos.dominio;

/**
 * Estado de una sanción por retraso (RN-03).
 * Se persiste como texto con el nombre exacto de la constante
 * (ver docs/agent-context/modelo-datos.md). No renombrar constantes sin una
 * migración que actualice los datos y el CHECK correspondiente.
 */
public enum EstadoSancion {
    ACTIVA,
    CUMPLIDA;
}
