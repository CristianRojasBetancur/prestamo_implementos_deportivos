package co.edu.itm.prestamosdeportivos.aplicacion;

import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;

/** Puerto del flujo de incidencias para retirar una unidad de disponibilidad. */
public interface IncidenciaDisponibilidad {

    void marcarNoDisponible(ImplementoDeportivo unidad, EstadoImplemento destino, Usuario operador, String motivo);
}
