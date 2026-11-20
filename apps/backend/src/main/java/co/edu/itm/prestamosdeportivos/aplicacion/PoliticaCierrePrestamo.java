package co.edu.itm.prestamosdeportivos.aplicacion;

import java.time.Instant;

import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import org.springframework.stereotype.Component;

/** Política única para cerrar o conservar activo un préstamo. */
@Component
public class PoliticaCierrePrestamo {

    public void actualizar(Prestamo prestamo, Instant ahora) {
        prestamo.actualizarEstadoTrasDevolucion(ahora);
    }
}
