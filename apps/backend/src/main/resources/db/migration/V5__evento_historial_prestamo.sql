ALTER TABLE historial_estado_implemento DROP CONSTRAINT ck_historial_estado_evento;
ALTER TABLE historial_estado_implemento ADD CONSTRAINT ck_historial_estado_evento
    CHECK (evento IN ('ADMINISTRACION', 'PRESTAMO', 'DEVOLUCION', 'INCIDENCIA'));
