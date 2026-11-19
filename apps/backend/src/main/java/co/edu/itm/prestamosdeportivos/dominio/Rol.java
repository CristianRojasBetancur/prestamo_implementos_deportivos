package co.edu.itm.prestamosdeportivos.dominio;

/**
 * Rol único de un {@link Usuario} (BE-01 §3.1).
 * Se persiste como texto con el nombre exacto de la constante
 * (ver docs/agent-context/modelo-datos.md). No renombrar constantes sin una
 * migración que actualice los datos y el CHECK correspondiente.
 */
public enum Rol {
    USER,
    PRESTADOR,
    ADMIN;
}
