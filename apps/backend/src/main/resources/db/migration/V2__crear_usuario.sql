CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    identificacion VARCHAR(40) NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    correo VARCHAR(254),
    contrasena_hash VARCHAR(100),
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version_credenciales BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMPTZ NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_usuario_identificacion UNIQUE (identificacion),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('USER', 'PRESTADOR', 'ADMIN')),
    CONSTRAINT ck_usuario_credenciales CHECK (
        (rol = 'USER' AND contrasena_hash IS NULL)
        OR (rol IN ('PRESTADOR', 'ADMIN') AND correo IS NOT NULL AND contrasena_hash IS NOT NULL)
    )
);

CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE UNIQUE INDEX uk_usuario_correo_ci ON usuario (LOWER(correo)) WHERE correo IS NOT NULL;
CREATE INDEX ix_usuario_rol_nombres ON usuario (rol, nombres, apellidos, id);
CREATE INDEX ix_usuario_rol_activo ON usuario (rol, activo);
