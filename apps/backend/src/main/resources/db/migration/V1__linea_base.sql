-- V1: línea base del esquema.
-- Inicializa el historial de Flyway sin crear objetos de negocio. Las tablas del
-- dominio (BE-01 §6 y §11) se agregan en migraciones posteriores (V2, V3, ...)
-- junto con el caso de uso que las necesita. Nunca se edita una migración ya
-- aplicada: los cambios se hacen con una migración nueva.
SELECT 1;
