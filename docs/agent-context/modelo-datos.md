# Modelo de datos y persistencia

Contexto operativo del esquema PostgreSQL del backend. Lo crean las migraciones
`apps/backend/src/main/resources/db/migration/V2__crear_esquema_mvp.sql`, `V3__historial_estado_implemento.sql` y `V4__indices_consulta_disponibilidad.sql` y lo
gobierna `docs/specs/BE-01-api-backend-mvp.md` (§6, §7 y §11). Es un documento
vivo: cualquier migración que cambie tablas, constraints, índices o enumeraciones
debe actualizarlo.

## 1. Tablas

Los nombres siguen BE-01 §4.2: español, `snake_case` y singular. La columna
"Issue" muestra el nombre que usa el issue de persistencia, para trazabilidad.

| Tabla | Issue | Entidad JPA | Contenido |
|---|---|---|---|
| `usuario` | `users` | `Usuario` | Solicitantes (`USER`), prestadores y administradores. |
| `categoria` | `categories` | `Categoria` | Clasificación de implementos. |
| `implemento_deportivo` | `equipment_units` | `ImplementoDeportivo` | Una fila por unidad física. |
| `historial_estado_implemento` | — | `HistorialEstadoImplemento` | Auditoría inmutable de estado anterior/nuevo, evento, motivo y fecha de cambio de una unidad. |
| `prestamo` | `loans` | `Prestamo` | Cabecera del préstamo. |
| `detalle_prestamo` | `loan_items` | `DetallePrestamo` | Unidad física dentro de un préstamo y su resolución. |
| `devolucion` | `returns` | `Devolucion` | Registro de una devolución total o parcial. |
| `detalle_devolucion` | `return_items` | `DetalleDevolucion` | Unidad recibida, condición e incidencia asociada. |
| `incidencia` | `incidents` | `Incidencia` | Daño, pérdida, robo u otra novedad. |
| `sancion` | `sanctions` | `Sancion` | Suspensión por devolución tardía (RN-03). |

Columnas comunes a todas las tablas:

- `id uuid`: PK generada por la aplicación (`GenerationType.UUID`), con
  `DEFAULT gen_random_uuid()` para cargas SQL.
- `creado_en` y `actualizado_en`: `timestamptz` de auditoría técnica
  (`@CreationTimestamp` / `@UpdateTimestamp`). Las fechas de negocio
  (`fecha_prestamo`, `fecha_limite_devolucion`, `fecha_devolucion`, etc.) son
  columnas propias que la aplicación calcula con el `Clock` inyectado.

## 2. Mapeo de enumeraciones

Toda enumeración de dominio se guarda como `varchar` con el **nombre exacto de
la constante Java** (`@Enumerated(EnumType.STRING)`), y un `CHECK` en
PostgreSQL limita los valores. No se usan tipos `ENUM` nativos de PostgreSQL ni
ordinales.

| Enum Java | Columna | Valores |
|---|---|---|
| `Rol` | `usuario.rol` | `USER`, `PRESTADOR`, `ADMIN` |
| `EstadoImplemento` | `implemento_deportivo.estado` | `DISPONIBLE`, `PRESTADO`, `EN_MANTENIMIENTO`, `DADO_DE_BAJA` |
| `EventoEstadoImplemento` | `historial_estado_implemento.evento` | `ADMINISTRACION`, `DEVOLUCION`, `INCIDENCIA` |
| `EstadoPrestamo` | `prestamo.estado` | `VIGENTE`, `DEVOLUCION_PARCIAL`, `VENCIDO`, `FINALIZADO` |
| `EstadoDetallePrestamo` | `detalle_prestamo.estado` | `PENDIENTE_DEVOLUCION`, `DEVUELTO`, `RESUELTO_POR_INCIDENCIA` |
| `CondicionDevolucion` | `detalle_prestamo.condicion_recibida`, `detalle_devolucion.condicion` | `BUENO`, `CON_NOVEDAD` |
| `TipoIncidencia` | `incidencia.tipo` | `DANO`, `PERDIDA`, `ROBO`, `OTRA` |
| `EstadoIncidencia` | `incidencia.estado` | `REGISTRADA`, `RESUELTA` |
| `AlcanceDevolucion` | `devolucion.alcance` | `PARCIAL`, `COMPLETA` |
| `EstadoSancion` | `sancion.estado` | `ACTIVA`, `CUMPLIDA` |

**Por qué `varchar + CHECK`:** es estable, legible en consultas manuales y
compatible con la validación de Hibernate (`ddl-auto: validate`). Agregar un
valor a un `ENUM` nativo no se puede hacer dentro de una transacción en todas
las versiones, mientras que reemplazar un `CHECK` sí.

**Cambiar una enumeración:** se crea una migración nueva que (1) actualiza los
datos existentes si se renombra un valor y (2) reemplaza el `CHECK`
(`ALTER TABLE ... DROP CONSTRAINT ck_...; ALTER TABLE ... ADD CONSTRAINT ck_... CHECK (...)`).
Nunca se renombra una constante Java sin esa migración.

## 3. Integridad garantizada por PostgreSQL

| Regla | Mecanismo |
|---|---|
| RN-06: una unidad no puede estar pendiente en dos préstamos | Índice único parcial `uq_detalle_prestamo_implemento_pendiente` sobre `detalle_prestamo(implemento_id) WHERE estado = 'PENDIENTE_DEVOLUCION'`. |
| Una unidad no se repite dentro del mismo préstamo | `uq_detalle_prestamo_prestamo_implemento`. |
| Una unidad de un préstamo se devuelve una sola vez | `uq_detalle_devolucion_detalle_prestamo`. |
| Relaciones válidas | Todas las FK son `NOT NULL` salvo `detalle_devolucion.incidencia_id`. Ninguna usa `ON DELETE CASCADE`. |
| El historial no se borra | Las FK restringen el borrado. Desactivar un prestador (`usuario.activo`) o retirar una unidad (`estado = DADO_DE_BAJA`) son actualizaciones. |
| La incidencia es coherente con su detalle | FK compuesta `fk_incidencia_detalle_prestamo (detalle_prestamo_id, prestamo_id, implemento_id)`. |
| Credenciales por rol | `ck_usuario_credenciales`: `USER` sin contraseña; `PRESTADOR`/`ADMIN` con correo y contraseña. |
| Unicidad sin distinguir mayúsculas | `uq_usuario_correo (lower(correo))` y `uq_categoria_nombre (lower(nombre))`. |
| Unicidad exacta | `uq_usuario_identificacion`, `uq_implemento_codigo_inventario` y los códigos legibles. |
| Auditoría de estados de unidad | `historial_estado_implemento` referencia la unidad sin cascada y conserva estado anterior/nuevo, evento y motivo. |
| Consulta pública de disponibilidad | `ix_implemento_catalogo_activo` cubre categoría/nombre/estado de unidades no retiradas; `ix_incidencia_registrada_implemento` acelera la exclusión de incidencias abiertas. |
| RN-02: plazo coherente | `ck_prestamo_fechas`: `fecha_limite_devolucion > fecha_prestamo`. |
| RN-07: finalización coherente | `ck_prestamo_finalizacion`: `estado = FINALIZADO` si y solo si hay `fecha_finalizacion`. |
| Resolución coherente del detalle | `ck_detalle_prestamo_resolucion`: pendiente sin fecha ni condición; `DEVUELTO` con ambas; `RESUELTO_POR_INCIDENCIA` con fecha y sin condición. |
| Novedad exige incidencia (CU-16) | `ck_detalle_devolucion_incidencia`: `CON_NOVEDAD` si y solo si hay `incidencia_id`. |
| Justificación de retraso | `ck_devolucion_justificacion`: si `retraso_justificado`, la justificación es obligatoria. |
| RN-03: mínimo 2 días de sanción | `ck_sancion_fechas`: `fecha_fin >= fecha_inicio + 2 días`. |

**Se valida en la capa de aplicación (no es viable con CHECK simples):**

- que `prestamo.usuario_id` tenga rol `USER` y `prestamo.prestador_id` rol
  `PRESTADOR`, y lo mismo en devoluciones, incidencias y sanciones;
- que el `detalle_prestamo` de un `detalle_devolucion` pertenezca al mismo
  préstamo de la devolución;
- los límites RN-01 (4 unidades) y RN-05 (vencidos y suspensión);
- la concurrencia del registro de préstamos: bloqueo `PESSIMISTIC_WRITE` en
  orden de UUID (`ImplementoDeportivoRepositorio.bloquearPorIds`) más el índice
  único parcial de RN-06 como última barrera; `@Version` en
  `implemento_deportivo` y `prestamo` para la edición concurrente.

## 4. Decisiones de mapeo

| ID | Decisión |
|---|---|
| `DM-01` | **Nombres en español (BE-01) en lugar de los del issue.** BE-01 §4.2 y §11 son la autoridad. La tabla de §1 mantiene la equivalencia. |
| `DM-02` | **Las entidades JPA viven en `dominio`.** Para evitar duplicar el modelo y escribir mappers sin necesidad en el MVP, las entidades de dominio usan anotaciones `jakarta.persistence` (sin dependencias de Spring). Los controladores nunca las exponen: usan DTO (BE-01 §12). |
| `DM-03` | **`usuario.suspendido_hasta` es una caché.** La fuente de verdad son las filas de `sancion`. El servicio que crea una sanción actualiza `suspendido_hasta` en la misma transacción con la mayor `fecha_fin` activa. Permite evaluar RN-05 sin consultar el historial. |
| `DM-04` | **`detalle_prestamo` guarda `fecha_resolucion` y `condicion_recibida`** (pedido por el issue). Son el estado actual de la unidad dentro del préstamo; `detalle_devolucion` conserva el registro del evento de devolución. Un `CHECK` mantiene la coherencia. |
| `DM-05` | **`incidencia` replica `prestamo_id` e `implemento_id`** del detalle para consultar por préstamo o por unidad sin joins. La FK compuesta impide incoherencias. |
| `DM-06` | **Códigos legibles generados por PostgreSQL** (`PRE-000001`, `DEV-…`, `INC-…`) con secuencias `MAXVALUE 999999`: al agotarse, la inserción falla en lugar de truncar el código. Puede haber saltos en la numeración (una inserción fallida consume un número); es normal y no afecta la unicidad. En JPA se mapean con `@Generated` e `insertable = false`. |
| `DM-07` | **La fecha de registro del implemento es `creado_en`.** BE-01 llama `fechaRegistro` a este dato en la API; no se duplica la columna. |
| `DM-08` | **`sancion.justificacion` se crea, pero es opcional.** Según BE-01 (RN-03, D-08), un retraso justificado no genera sanción, así que en el MVP la columna queda `NULL`. Se conserva porque el issue la pide, para observaciones o una futura anulación (ver `PEND-05`). |
| `DM-09` | **Instantes en `timestamptz` (UTC)** con `hibernate.jdbc.time_zone = UTC`. La zona institucional solo se aplica al serializar respuestas (BE-01 §4.4). |

**Pendiente para el equipo:**

- `PEND-05`: confirmar el uso de `sancion.justificacion` (anulación de sanciones
  u observaciones). BE-01 no define ese flujo.

## 5. Índices para los filtros principales

| Índice | Consulta que atiende |
|---|---|
| `ix_prestamo_usuario_estado` | Préstamos activos de un usuario (RN-01, RN-05, CU-13). |
| `ix_prestamo_vencimiento` (parcial, `estado <> 'FINALIZADO'`) | Préstamos vencidos (RN-05, tarea CU-20). |
| `ix_prestamo_fecha_prestamo`, `ix_devolucion_fecha_devolucion`, `ix_incidencia_fecha_registro` | Listados ordenados por fecha y filtros `desde`/`hasta`. |
| `ix_implemento_categoria_estado`, `ix_implemento_estado` | Disponibilidad por categoría e inventario por estado. |
| `ix_detalle_prestamo_prestamo_estado`, `uq_detalle_prestamo_implemento_pendiente` | Unidades pendientes de un préstamo o de un usuario. |
| `ix_sancion_usuario_vigente` (parcial, `estado = 'ACTIVA'`) | Sanción vigente de un usuario. |
| `uq_usuario_identificacion`, `uq_usuario_correo`, `uq_implemento_codigo_inventario`, `uq_*_codigo` | Búsquedas exactas por identificación, correo y códigos. |

La búsqueda de texto libre `q` (BE-01 §4.6) sobre nombres usará `ILIKE`. Si la
prueba de carga de RNF-01 lo exige, se agregará un índice `pg_trgm` en una
migración posterior.

**Revisar un plan** (en la base local de `compose.yaml`):

```bash
docker compose exec postgres psql -U prestamos -d prestamos
```

```sql
EXPLAIN SELECT id FROM prestamo WHERE usuario_id = '<uuid>' AND estado <> 'FINALIZADO';
EXPLAIN SELECT id FROM prestamo WHERE estado <> 'FINALIZADO' AND fecha_limite_devolucion < now();
```

Con pocas filas, PostgreSQL prefiere un *Seq Scan*. Para comprobar que el índice
es utilizable, ejecute antes `SET enable_seqscan = off;`. La prueba
`EsquemaPersistenciaTest.PlanesDeConsulta` lo verifica automáticamente.

## 6. Migraciones: aplicación y reversión

- **Desde cero:** con una base vacía, Flyway aplica V1 y V2 al arrancar
  (`mvn spring-boot:run`) o en las pruebas (`mvn test` con Testcontainers).
- **Incremental:** una base que ya tiene V1 (por ejemplo, la de `compose.yaml`
  creada con la inicialización del backend) recibe solo V2 en el siguiente
  arranque. Se comprueba en `flyway_schema_history`.
- **Reversión: estrategia forward-only.** Flyway Community no soporta *undo*
  automático. Una migración aplicada **nunca se edita** (Flyway detecta el
  cambio de checksum y la aplicación no arranca). Para revertir:
  - en un entorno compartido o productivo, se escribe una migración correctiva
    nueva (`V3__...`) que deshace el cambio;
  - en desarrollo local, se recrea la base:
    `docker compose down -v && docker compose up -d`, y se arranca de nuevo.
- Las migraciones se ejecutan en una transacción por archivo (PostgreSQL admite
  DDL transaccional): si una sentencia falla, la migración completa se revierte.
