package co.edu.itm.prestamosdeportivos.persistencia;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;

public record PrestamoFiltros(Set<EstadoPrestamo> estados, UUID usuarioId, String identificacion,
        Instant desde, Instant hasta, String texto, Instant ahora) {
}
