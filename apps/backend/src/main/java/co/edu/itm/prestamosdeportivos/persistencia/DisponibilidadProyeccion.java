package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.UUID;

/** Proyección agregada del catálogo público; no contiene identificadores ni códigos de unidades. */
public interface DisponibilidadProyeccion {

    String getNombre();
    UUID getCategoriaId();
    String getCategoriaNombre();
    long getUnidadesDisponibles();
    long getUnidadesTotales();
    boolean getDisponible();
}
