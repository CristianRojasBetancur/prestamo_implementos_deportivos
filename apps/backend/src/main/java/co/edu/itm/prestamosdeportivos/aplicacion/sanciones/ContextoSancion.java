package co.edu.itm.prestamosdeportivos.aplicacion.sanciones;

import java.time.Instant;

import co.edu.itm.prestamosdeportivos.dominio.Devolucion;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;

/** Datos inmutables que una política necesita para evaluar RN-03. */
public record ContextoSancion(Prestamo prestamo, Devolucion devolucion, Instant ahora) {
}
