CREATE TABLE historial_estado_implemento (
    id              uuid         NOT NULL DEFAULT gen_random_uuid(),
    implemento_id   uuid         NOT NULL,
    estado_anterior varchar(20)  NOT NULL,
    estado_nuevo    varchar(20)  NOT NULL,
    evento          varchar(20)  NOT NULL,
    motivo          varchar(500) NOT NULL,
    creado_en       timestamptz  NOT NULL DEFAULT now(),
    actualizado_en  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_historial_estado_implemento PRIMARY KEY (id),
    CONSTRAINT fk_historial_estado_implemento FOREIGN KEY (implemento_id) REFERENCES implemento_deportivo (id),
    CONSTRAINT ck_historial_estado_evento CHECK (evento IN ('ADMINISTRACION', 'DEVOLUCION', 'INCIDENCIA')),
    CONSTRAINT ck_historial_estado_anterior CHECK (estado_anterior IN ('DISPONIBLE', 'PRESTADO', 'EN_MANTENIMIENTO', 'DADO_DE_BAJA')),
    CONSTRAINT ck_historial_estado_nuevo CHECK (estado_nuevo IN ('DISPONIBLE', 'PRESTADO', 'EN_MANTENIMIENTO', 'DADO_DE_BAJA')),
    CONSTRAINT ck_historial_estado_diferente CHECK (estado_anterior <> estado_nuevo)
);

CREATE INDEX ix_historial_estado_implemento_fecha ON historial_estado_implemento (implemento_id, creado_en);
