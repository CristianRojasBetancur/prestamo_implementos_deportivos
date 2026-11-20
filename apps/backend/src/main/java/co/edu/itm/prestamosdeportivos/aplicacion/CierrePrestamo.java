package co.edu.itm.prestamosdeportivos.aplicacion;

import java.time.Instant;

import co.edu.itm.prestamosdeportivos.dominio.Prestamo;

/** Puerto para evaluar el estado global de un préstamo después de resolver unidades. */
public interface CierrePrestamo {

    void actualizar(Prestamo prestamo, Instant ahora);
}
