package co.edu.itm.prestamosdeportivos.dominio;

/**
 * Tipo de novedad registrada sobre una unidad (BE-01 §6.4).
 * Se persiste como texto con el nombre exacto de la constante
 * (ver docs/agent-context/modelo-datos.md). No renombrar constantes sin una
 * migración que actualice los datos y el CHECK correspondiente.
 */
public enum TipoIncidencia {
    DANO,
    PERDIDA,
    ROBO,
    OTRA;
}
