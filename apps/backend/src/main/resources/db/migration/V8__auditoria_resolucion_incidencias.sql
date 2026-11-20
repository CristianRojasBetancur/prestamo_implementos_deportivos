-- Auditoría del usuario que resuelve una incidencia. El historial existente no se borra.
ALTER TABLE incidencia
    ADD COLUMN resuelto_por_id uuid;

ALTER TABLE incidencia
    ADD CONSTRAINT fk_incidencia_resuelto_por
    FOREIGN KEY (resuelto_por_id) REFERENCES usuario (id);

CREATE INDEX ix_incidencia_estado_fecha ON incidencia (estado, fecha_registro DESC);
