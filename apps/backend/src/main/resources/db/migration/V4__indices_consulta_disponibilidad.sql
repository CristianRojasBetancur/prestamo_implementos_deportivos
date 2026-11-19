-- Cobertura del catálogo agregado que excluye bajas, e incidencias abiertas.
CREATE INDEX ix_implemento_catalogo_activo
    ON implemento_deportivo (categoria_id, nombre) INCLUDE (estado)
    WHERE estado <> 'DADO_DE_BAJA';

CREATE INDEX ix_incidencia_registrada_implemento
    ON incidencia (implemento_id)
    WHERE estado = 'REGISTRADA';
