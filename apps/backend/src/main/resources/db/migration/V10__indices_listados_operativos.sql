-- Índices compuestos para los filtros más selectivos de los listados operativos.
-- El segundo criterio conserva el orden descendente usado por la API y el id
-- aporta un desempate estable para la paginación.
CREATE INDEX ix_prestamo_estado_fecha_id
    ON prestamo (estado, fecha_prestamo DESC, id);

CREATE INDEX ix_devolucion_alcance_fecha_id
    ON devolucion (alcance, fecha_devolucion DESC, id);

CREATE INDEX ix_incidencia_tipo_fecha_id
    ON incidencia (tipo, fecha_registro DESC, id);
