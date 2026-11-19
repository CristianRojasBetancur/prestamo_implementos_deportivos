-- Índices para los filtros históricos de CU-17/BE-12.
CREATE INDEX ix_devolucion_alcance ON devolucion (alcance);
CREATE INDEX ix_detalle_devolucion_condicion ON detalle_devolucion (condicion);
CREATE INDEX ix_detalle_prestamo_implemento ON detalle_prestamo (implemento_id);
