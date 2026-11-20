# ADR-001: Operación y verificación del backend MVP

## Estado

Aceptada.

## Decisiones

- La imagen del backend se construye en dos etapas y se ejecuta con un usuario
  no root en un JRE 21 mínimo.
- PostgreSQL local y la API se levantan mediante el Compose oficial de la raíz.
- Flyway es la única autoridad para el esquema; Hibernate valida con
  `ddl-auto: validate`.
- Las pruebas de integración usan PostgreSQL efímero con Testcontainers y las
  migraciones se validan desde una base vacía.
- Préstamos, devoluciones e incidencias conservan sus límites transaccionales;
  la concurrencia de una unidad se protege con bloqueo e índice único parcial.
- Los estados y transiciones permanecen en el dominio. Elegibilidad usa Chain
  of Responsibility, sanciones usan Strategy y la coordinación de inventario
  usa Mediator.
- La seguridad es stateless, deny-by-default, con JWT, roles cargados desde la
  base y controles de ruta más `@PreAuthorize`.
- RF-19 no se implementa como requisito nuevo. La discrepancia editorial queda
  documentada en D-02 y la consulta de devoluciones se traza a HU-14/RU-14/CU-17.

## Consecuencias

El entorno local es reproducible y verificable, pero no demuestra por sí mismo
la disponibilidad mensual de 99,5 %. El rendimiento requiere conservar una
ejecución k6 con dataset y configuración identificables.
