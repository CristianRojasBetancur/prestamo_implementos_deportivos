package co.edu.itm.prestamosdeportivos.persistencia;

import java.time.Instant;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoIncidencia;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;

/** Filtros del listado histórico de incidencias. */
public record IncidenciaFiltros(UUID prestamoId, UUID usuarioId, UUID implementoId,
        TipoIncidencia tipo, EstadoIncidencia estado, Instant desde, Instant hasta, String texto) {
}
