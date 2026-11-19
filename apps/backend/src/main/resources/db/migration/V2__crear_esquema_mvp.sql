-- =============================================================================
-- V2: esquema del MVP (BE-01 §6 y §11; decisiones en docs/agent-context/modelo-datos.md)
--
-- Convenciones:
--   * Tablas y columnas en español, snake_case, singular (BE-01 §4.2).
--   * Identificadores UUID (generados por la aplicación; DEFAULT para cargas SQL).
--   * Instantes en timestamptz (UTC). Enumeraciones como varchar + CHECK con el
--     nombre exacto del enum Java (@Enumerated(EnumType.STRING)).
--   * Prefijos: pk_, fk_, uq_, ck_, ix_.
--   * Las FK no usan ON DELETE CASCADE: el historial no se borra. Desactivar un
--     prestador o retirar una unidad son actualizaciones, nunca borrados.
-- =============================================================================

-- Secuencias para los códigos legibles (BE-01 §4.3). MAXVALUE evita que un
-- código se trunque en silencio: al agotarse, la inserción falla.
CREATE SEQUENCE seq_codigo_prestamo   AS integer START 1 MAXVALUE 999999 NO CYCLE;
CREATE SEQUENCE seq_codigo_devolucion AS integer START 1 MAXVALUE 999999 NO CYCLE;
CREATE SEQUENCE seq_codigo_incidencia AS integer START 1 MAXVALUE 999999 NO CYCLE;

-- -----------------------------------------------------------------------------
-- usuario (issue: users)
-- -----------------------------------------------------------------------------
CREATE TABLE usuario (
    id                   uuid         NOT NULL DEFAULT gen_random_uuid(),
    identificacion       varchar(30)  NOT NULL,
    nombres              varchar(100) NOT NULL,
    apellidos            varchar(100) NOT NULL,
    correo               varchar(254),
    contrasena_hash      varchar(100),
    rol                  varchar(20)  NOT NULL,
    activo               boolean      NOT NULL DEFAULT true,
    suspendido_hasta     timestamptz,
    version_credenciales integer      NOT NULL DEFAULT 0,
    creado_en            timestamptz  NOT NULL DEFAULT now(),
    actualizado_en       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_identificacion UNIQUE (identificacion),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('USER', 'PRESTADOR', 'ADMIN')),
    -- PRESTADOR y ADMIN inician sesión: correo y contraseña obligatorios.
    -- USER no inicia sesión en el MVP: no tiene contraseña (BE-01 §5).
    CONSTRAINT ck_usuario_credenciales CHECK (
        (rol = 'USER' AND contrasena_hash IS NULL)
        OR (rol <> 'USER' AND correo IS NOT NULL AND contrasena_hash IS NOT NULL)
    ),
    CONSTRAINT ck_usuario_version_credenciales CHECK (version_credenciales >= 0)
);

-- Correo único sin distinguir mayúsculas; varios NULL permitidos (usuarios USER).
CREATE UNIQUE INDEX uq_usuario_correo ON usuario (lower(correo)) WHERE correo IS NOT NULL;
CREATE INDEX ix_usuario_rol_activo ON usuario (rol, activo);

-- -----------------------------------------------------------------------------
-- categoria (issue: categories)
-- -----------------------------------------------------------------------------
CREATE TABLE categoria (
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    nombre         varchar(80)  NOT NULL,
    descripcion    varchar(500),
    activa         boolean      NOT NULL DEFAULT true,
    creado_en      timestamptz  NOT NULL DEFAULT now(),
    actualizado_en timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_categoria PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uq_categoria_nombre ON categoria (lower(nombre));

-- -----------------------------------------------------------------------------
-- implemento_deportivo (issue: equipment_units) — una fila por unidad física
-- -----------------------------------------------------------------------------
CREATE TABLE implemento_deportivo (
    id                uuid          NOT NULL DEFAULT gen_random_uuid(),
    codigo_inventario varchar(30)   NOT NULL,
    nombre            varchar(120)  NOT NULL,
    descripcion       varchar(1000),
    categoria_id      uuid          NOT NULL,
    estado            varchar(20)   NOT NULL DEFAULT 'DISPONIBLE',
    version           bigint        NOT NULL DEFAULT 0,
    creado_en         timestamptz   NOT NULL DEFAULT now(),
    actualizado_en    timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_implemento_deportivo PRIMARY KEY (id),
    CONSTRAINT uq_implemento_codigo_inventario UNIQUE (codigo_inventario),
    CONSTRAINT fk_implemento_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT ck_implemento_estado CHECK (estado IN ('DISPONIBLE', 'PRESTADO', 'EN_MANTENIMIENTO', 'DADO_DE_BAJA'))
);

CREATE INDEX ix_implemento_categoria_estado ON implemento_deportivo (categoria_id, estado);
CREATE INDEX ix_implemento_estado ON implemento_deportivo (estado);

-- -----------------------------------------------------------------------------
-- prestamo (issue: loans)
-- -----------------------------------------------------------------------------
CREATE TABLE prestamo (
    id                      uuid        NOT NULL DEFAULT gen_random_uuid(),
    codigo                  varchar(12) NOT NULL
                            DEFAULT 'PRE-' || lpad(nextval('seq_codigo_prestamo')::text, 6, '0'),
    usuario_id              uuid        NOT NULL,
    prestador_id            uuid        NOT NULL,
    fecha_prestamo          timestamptz NOT NULL,
    fecha_limite_devolucion timestamptz NOT NULL,
    fecha_finalizacion      timestamptz,
    estado                  varchar(20) NOT NULL DEFAULT 'VIGENTE',
    version                 bigint      NOT NULL DEFAULT 0,
    creado_en               timestamptz NOT NULL DEFAULT now(),
    actualizado_en          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_prestamo PRIMARY KEY (id),
    CONSTRAINT uq_prestamo_codigo UNIQUE (codigo),
    CONSTRAINT fk_prestamo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_prestamo_prestador FOREIGN KEY (prestador_id) REFERENCES usuario (id),
    CONSTRAINT ck_prestamo_estado CHECK (estado IN ('VIGENTE', 'DEVOLUCION_PARCIAL', 'VENCIDO', 'FINALIZADO')),
    CONSTRAINT ck_prestamo_fechas CHECK (fecha_limite_devolucion > fecha_prestamo),
    CONSTRAINT ck_prestamo_finalizacion CHECK ((estado = 'FINALIZADO') = (fecha_finalizacion IS NOT NULL)),
    CONSTRAINT ck_prestamo_usuario_distinto CHECK (usuario_id <> prestador_id)
);

CREATE INDEX ix_prestamo_usuario_estado ON prestamo (usuario_id, estado);
CREATE INDEX ix_prestamo_prestador ON prestamo (prestador_id);
CREATE INDEX ix_prestamo_fecha_prestamo ON prestamo (fecha_prestamo DESC);
-- Vencimientos pendientes (RN-02, RN-05, CU-20).
CREATE INDEX ix_prestamo_vencimiento ON prestamo (fecha_limite_devolucion) WHERE estado <> 'FINALIZADO';

-- -----------------------------------------------------------------------------
-- detalle_prestamo (issue: loan_items) — una unidad física dentro de un préstamo
-- -----------------------------------------------------------------------------
CREATE TABLE detalle_prestamo (
    id                 uuid        NOT NULL DEFAULT gen_random_uuid(),
    prestamo_id        uuid        NOT NULL,
    implemento_id      uuid        NOT NULL,
    estado             varchar(30) NOT NULL DEFAULT 'PENDIENTE_DEVOLUCION',
    fecha_resolucion   timestamptz,
    condicion_recibida varchar(20),
    creado_en          timestamptz NOT NULL DEFAULT now(),
    actualizado_en     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_detalle_prestamo PRIMARY KEY (id),
    CONSTRAINT uq_detalle_prestamo_prestamo_implemento UNIQUE (prestamo_id, implemento_id),
    -- Destino de la FK compuesta de incidencia (garantiza coherencia préstamo/unidad).
    CONSTRAINT uq_detalle_prestamo_id_prestamo_implemento UNIQUE (id, prestamo_id, implemento_id),
    CONSTRAINT fk_detalle_prestamo_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamo (id),
    CONSTRAINT fk_detalle_prestamo_implemento FOREIGN KEY (implemento_id) REFERENCES implemento_deportivo (id),
    CONSTRAINT ck_detalle_prestamo_estado CHECK (estado IN ('PENDIENTE_DEVOLUCION', 'DEVUELTO', 'RESUELTO_POR_INCIDENCIA')),
    CONSTRAINT ck_detalle_prestamo_condicion CHECK (condicion_recibida IN ('BUENO', 'CON_NOVEDAD')),
    CONSTRAINT ck_detalle_prestamo_resolucion CHECK (
        (estado = 'PENDIENTE_DEVOLUCION' AND fecha_resolucion IS NULL AND condicion_recibida IS NULL)
        OR (estado = 'DEVUELTO' AND fecha_resolucion IS NOT NULL AND condicion_recibida IS NOT NULL)
        OR (estado = 'RESUELTO_POR_INCIDENCIA' AND fecha_resolucion IS NOT NULL AND condicion_recibida IS NULL)
    )
);

-- RN-06: una unidad física solo puede estar pendiente en un préstamo a la vez.
CREATE UNIQUE INDEX uq_detalle_prestamo_implemento_pendiente
    ON detalle_prestamo (implemento_id) WHERE estado = 'PENDIENTE_DEVOLUCION';
CREATE INDEX ix_detalle_prestamo_prestamo_estado ON detalle_prestamo (prestamo_id, estado);

-- -----------------------------------------------------------------------------
-- incidencia (issue: incidents)
-- -----------------------------------------------------------------------------
CREATE TABLE incidencia (
    id                  uuid          NOT NULL DEFAULT gen_random_uuid(),
    codigo              varchar(12)   NOT NULL
                        DEFAULT 'INC-' || lpad(nextval('seq_codigo_incidencia')::text, 6, '0'),
    detalle_prestamo_id uuid          NOT NULL,
    prestamo_id         uuid          NOT NULL,
    implemento_id       uuid          NOT NULL,
    prestador_id        uuid          NOT NULL,
    tipo                varchar(20)   NOT NULL,
    descripcion         varchar(1000) NOT NULL,
    estado              varchar(20)   NOT NULL DEFAULT 'REGISTRADA',
    fecha_registro      timestamptz   NOT NULL,
    fecha_resolucion    timestamptz,
    creado_en           timestamptz   NOT NULL DEFAULT now(),
    actualizado_en      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_incidencia PRIMARY KEY (id),
    CONSTRAINT uq_incidencia_codigo UNIQUE (codigo),
    CONSTRAINT fk_incidencia_detalle_prestamo FOREIGN KEY (detalle_prestamo_id, prestamo_id, implemento_id)
        REFERENCES detalle_prestamo (id, prestamo_id, implemento_id),
    CONSTRAINT fk_incidencia_prestador FOREIGN KEY (prestador_id) REFERENCES usuario (id),
    CONSTRAINT ck_incidencia_tipo CHECK (tipo IN ('DANO', 'PERDIDA', 'ROBO', 'OTRA')),
    CONSTRAINT ck_incidencia_estado CHECK (estado IN ('REGISTRADA', 'RESUELTA')),
    CONSTRAINT ck_incidencia_resolucion CHECK ((estado = 'RESUELTA') = (fecha_resolucion IS NOT NULL)),
    CONSTRAINT ck_incidencia_descripcion CHECK (length(trim(descripcion)) > 0)
);

CREATE INDEX ix_incidencia_prestamo ON incidencia (prestamo_id);
CREATE INDEX ix_incidencia_implemento ON incidencia (implemento_id);
CREATE INDEX ix_incidencia_detalle_prestamo ON incidencia (detalle_prestamo_id);
CREATE INDEX ix_incidencia_tipo ON incidencia (tipo);
CREATE INDEX ix_incidencia_fecha_registro ON incidencia (fecha_registro DESC);

-- -----------------------------------------------------------------------------
-- devolucion (issue: returns)
-- -----------------------------------------------------------------------------
CREATE TABLE devolucion (
    id                  uuid          NOT NULL DEFAULT gen_random_uuid(),
    codigo              varchar(12)   NOT NULL
                        DEFAULT 'DEV-' || lpad(nextval('seq_codigo_devolucion')::text, 6, '0'),
    prestamo_id         uuid          NOT NULL,
    prestador_id        uuid          NOT NULL,
    fecha_devolucion    timestamptz   NOT NULL,
    alcance             varchar(10)   NOT NULL,
    retraso_justificado boolean       NOT NULL DEFAULT false,
    justificacion       varchar(1000),
    observaciones       varchar(1000),
    creado_en           timestamptz   NOT NULL DEFAULT now(),
    actualizado_en      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_devolucion PRIMARY KEY (id),
    CONSTRAINT uq_devolucion_codigo UNIQUE (codigo),
    CONSTRAINT fk_devolucion_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamo (id),
    CONSTRAINT fk_devolucion_prestador FOREIGN KEY (prestador_id) REFERENCES usuario (id),
    CONSTRAINT ck_devolucion_alcance CHECK (alcance IN ('PARCIAL', 'COMPLETA')),
    CONSTRAINT ck_devolucion_justificacion CHECK (NOT retraso_justificado OR justificacion IS NOT NULL)
);

CREATE INDEX ix_devolucion_prestamo ON devolucion (prestamo_id);
CREATE INDEX ix_devolucion_fecha_devolucion ON devolucion (fecha_devolucion DESC);

-- -----------------------------------------------------------------------------
-- detalle_devolucion (issue: return_items)
-- -----------------------------------------------------------------------------
CREATE TABLE detalle_devolucion (
    id                  uuid          NOT NULL DEFAULT gen_random_uuid(),
    devolucion_id       uuid          NOT NULL,
    detalle_prestamo_id uuid          NOT NULL,
    condicion           varchar(20)   NOT NULL,
    observaciones       varchar(1000),
    incidencia_id       uuid,
    creado_en           timestamptz   NOT NULL DEFAULT now(),
    actualizado_en      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_detalle_devolucion PRIMARY KEY (id),
    -- Una unidad de un préstamo se devuelve una sola vez.
    CONSTRAINT uq_detalle_devolucion_detalle_prestamo UNIQUE (detalle_prestamo_id),
    CONSTRAINT uq_detalle_devolucion_incidencia UNIQUE (incidencia_id),
    CONSTRAINT fk_detalle_devolucion_devolucion FOREIGN KEY (devolucion_id) REFERENCES devolucion (id),
    CONSTRAINT fk_detalle_devolucion_detalle_prestamo FOREIGN KEY (detalle_prestamo_id) REFERENCES detalle_prestamo (id),
    CONSTRAINT fk_detalle_devolucion_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencia (id),
    CONSTRAINT ck_detalle_devolucion_condicion CHECK (condicion IN ('BUENO', 'CON_NOVEDAD')),
    -- BE-01 CU-16: CON_NOVEDAD exige incidencia; BUENO no la admite.
    CONSTRAINT ck_detalle_devolucion_incidencia CHECK ((condicion = 'CON_NOVEDAD') = (incidencia_id IS NOT NULL))
);

CREATE INDEX ix_detalle_devolucion_devolucion ON detalle_devolucion (devolucion_id);

-- -----------------------------------------------------------------------------
-- sancion (issue: sanctions)
-- -----------------------------------------------------------------------------
CREATE TABLE sancion (
    id              uuid         NOT NULL DEFAULT gen_random_uuid(),
    usuario_id      uuid         NOT NULL,
    prestamo_id     uuid         NOT NULL,
    devolucion_id   uuid         NOT NULL,
    motivo          varchar(500) NOT NULL,
    minutos_retraso bigint       NOT NULL,
    fecha_inicio    timestamptz  NOT NULL,
    fecha_fin       timestamptz  NOT NULL,
    justificacion   varchar(1000),
    estado          varchar(20)  NOT NULL DEFAULT 'ACTIVA',
    creado_en       timestamptz  NOT NULL DEFAULT now(),
    actualizado_en  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_sancion PRIMARY KEY (id),
    CONSTRAINT fk_sancion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_sancion_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamo (id),
    CONSTRAINT fk_sancion_devolucion FOREIGN KEY (devolucion_id) REFERENCES devolucion (id),
    CONSTRAINT ck_sancion_estado CHECK (estado IN ('ACTIVA', 'CUMPLIDA')),
    CONSTRAINT ck_sancion_retraso CHECK (minutos_retraso > 0),
    -- RN-03: la suspensión dura al menos 2 días.
    CONSTRAINT ck_sancion_fechas CHECK (fecha_fin >= fecha_inicio + interval '2 days')
);

CREATE INDEX ix_sancion_usuario_vigente ON sancion (usuario_id, fecha_fin DESC) WHERE estado = 'ACTIVA';
CREATE INDEX ix_sancion_prestamo ON sancion (prestamo_id);
