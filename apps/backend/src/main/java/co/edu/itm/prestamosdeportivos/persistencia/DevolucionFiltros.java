package co.edu.itm.prestamosdeportivos.persistencia;

import java.time.Instant;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.AlcanceDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;

/** Filtros de la consulta histórica de devoluciones (BE-12/CU-17). */
public record DevolucionFiltros(UUID prestamoId, UUID usuarioId, UUID implementoId,
        CondicionDevolucion condicion, AlcanceDevolucion alcance, Instant desde, Instant hasta, String texto) {
}
