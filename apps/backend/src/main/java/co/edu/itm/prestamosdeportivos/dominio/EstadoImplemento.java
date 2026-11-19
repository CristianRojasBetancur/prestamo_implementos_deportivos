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

    /** Transiciones válidas por clase de operación; evita restaurar desde un préstamo sin devolución. */
    public boolean admite(EstadoImplemento destino, EventoEstadoImplemento evento) {
        if (destino == null || evento == null || destino == this || this == DADO_DE_BAJA) {
            return false;
        }
        return switch (evento) {
            case PRESTAMO -> this == DISPONIBLE && destino == PRESTADO;
            case ADMINISTRACION -> switch (this) {
                case DISPONIBLE -> destino == EN_MANTENIMIENTO || destino == DADO_DE_BAJA;
                case EN_MANTENIMIENTO -> destino == DISPONIBLE || destino == DADO_DE_BAJA;
                case PRESTADO, DADO_DE_BAJA -> false;
            };
            case DEVOLUCION -> this == PRESTADO && destino == DISPONIBLE;
            case INCIDENCIA -> this == PRESTADO && (destino == EN_MANTENIMIENTO || destino == DADO_DE_BAJA);
        };
    }

    public boolean esPrestable() {
        return this == DISPONIBLE;
    }
}
