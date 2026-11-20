ALTER TABLE historial_estado_implemento
    ADD COLUMN operador_id uuid;

ALTER TABLE historial_estado_implemento
    ADD CONSTRAINT fk_historial_estado_implemento_operador
    FOREIGN KEY (operador_id) REFERENCES usuario (id);

CREATE INDEX ix_historial_estado_implemento_operador
    ON historial_estado_implemento (operador_id);
