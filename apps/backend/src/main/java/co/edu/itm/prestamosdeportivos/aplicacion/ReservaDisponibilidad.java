package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.Collection;

import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;

/** Puerto del flujo de préstamo para reservar unidades ya bloqueadas. */
public interface ReservaDisponibilidad {

    void reservar(Collection<ImplementoDeportivo> unidades, Usuario operador);
}
