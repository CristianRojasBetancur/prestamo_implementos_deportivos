package co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Usuario;

/** Datos de solo lectura que cada eslabón de la política evalúa. */
public record ContextoElegibilidad(Usuario usuario, Instant ahora, long unidadesActivas, long prestamosActivos,
        long prestamosVencidos, boolean sancionVigente, List<UUID> implementoIds, Instant fechaPrestamo,
        Instant fechaLimite) {
}
