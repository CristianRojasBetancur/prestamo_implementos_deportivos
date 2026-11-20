package co.edu.itm.prestamosdeportivos.aplicacion;

import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;

/** Puerto del flujo de devolución para resolver la disponibilidad de una unidad. */
public interface LiberacionDisponibilidad {

    void liberar(ImplementoDeportivo unidad, CondicionDevolucion condicion, Usuario operador);
}
