package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.UUID;

/**
 * Proyección mínima para validar varias unidades en una sola consulta.
 * Evita que la cadena de elegibilidad ejecute tres consultas por unidad.
 */
public interface DisponibilidadUnidad {

    UUID getId();

    String getCodigoInventario();

    String getEstado();

    Boolean getPendienteDevolucion();

    Boolean getIncidenciaRegistrada();
}
