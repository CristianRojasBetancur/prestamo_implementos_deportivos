package co.edu.itm.prestamosdeportivos.aplicacion.sanciones;

import java.time.Duration;

/** Resultado del cálculo de una política, antes de crear la entidad persistente. */
public record ResultadoSancion(long minutosRetraso, Duration duracion) {
}
