package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.UUID;

/** Fila agregada mapeada desde la consulta nativa paginada. */
record DisponibilidadFila(String nombre, UUID categoriaId, String categoriaNombre,
        long unidadesDisponibles, long unidadesTotales, boolean disponible) implements DisponibilidadProyeccion {

    @Override public String getNombre() { return nombre; }
    @Override public UUID getCategoriaId() { return categoriaId; }
    @Override public String getCategoriaNombre() { return categoriaNombre; }
    @Override public long getUnidadesDisponibles() { return unidadesDisponibles; }
    @Override public long getUnidadesTotales() { return unidadesTotales; }
    @Override public boolean getDisponible() { return disponible; }
}
