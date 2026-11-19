package co.edu.itm.prestamosdeportivos.dominio;

/**
 * Estado físico y operativo de una unidad (BE-01 §6.2).
 * Se persiste como texto con el nombre exacto de la constante
 * (ver docs/agent-context/modelo-datos.md). No renombrar constantes sin una
 * migración que actualice los datos y el CHECK correspondiente.
 */
public enum EstadoImplemento {
    DISPONIBLE,
    PRESTADO,
    EN_MANTENIMIENTO,
    DADO_DE_BAJA;
}
