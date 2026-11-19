# BE-08 — Consulta pública de disponibilidad

| Campo | Decisión |
|---|---|
| Estado | Implementado; pruebas de integración y carga pendientes de Docker/k6 |
| Actor | Público (usuario sin autenticación), según BE-01 §3.2 y D-03 |
| Trazabilidad | HU-08, RU-08, RF-10, RN-04, RNF-01; CU-12 de BE-01 |
| Dependencias | BE-03, BE-04 y BE-07; el esquema e inventario BE-07 son la fuente del estado actual |

## Objetivo

Consultar de forma paginada la disponibilidad vigente por categoría y nombre del implemento, sin revelar datos operativos de unidades físicas.

## Contrato

- `GET /api/v1/availability` es la ruta pedida por BE-08. `GET /api/v1/disponibilidad` se conserva como ruta canónica compatible con BE-01; ambas ejecutan el mismo caso de uso público.
- Filtros: `categoriaId` (nombre canónico BE-01) o `categoryId` (alias aceptado por BE-08), UUID opcional, y `q` opcional, búsqueda insensible a mayúsculas y tildes sobre nombre de implemento y nombre de categoría. Paginación estándar `pagina`, `tamano` (máximo 100), `orden`.
- Respuesta `PaginaRespuesta<DisponibilidadRespuesta>` de BE-01: `nombre`, referencia de categoría, `unidadesDisponibles`, `unidadesTotales` y `disponible`. Orden por defecto: nombre ascendente, categoría y UUID de categoría para desempatar de forma estable.
- La respuesta es agregada por (categoría, nombre). No incluye UUID, código, estado individual ni relación a préstamos/incidencias. Búsqueda por código no se expone: BE-01 §10.2 y D-03 prohíben revelar identificadores/códigos de unidades en la consulta pública.
- Solo se devuelven grupos con al menos una unidad actualmente `DISPONIBLE`; si no hay coincidencias, devuelve 200 con `contenido: []` y metadatos de página.
- `unidadesDisponibles` cuenta únicamente unidades `DISPONIBLE`. `unidadesTotales` cuenta unidades no dadas de baja, sin incidencia `REGISTRADA`; las unidades prestadas y en mantenimiento nunca aumentan la cantidad disponible.

## Reglas y persistencia

- Una unidad solo contribuye a disponibilidad si su estado persistido es `DISPONIBLE`, su categoría está activa y no tiene incidencia `REGISTRADA`. La transición transaccional a `PRESTADO` al confirmar un préstamo la excluye en la siguiente consulta.
- `EN_MANTENIMIENTO`, `DADO_DE_BAJA`, `PRESTADO` y unidades con incidencias abiertas no contribuyen a `unidadesDisponibles`. La baja tampoco contribuye al total.
- La consulta usa una agregación de base de datos y paginación de grupos; no carga unidades a memoria ni ejecuta consultas por fila.
- Reutiliza `ix_implemento_catalogo_activo` (categoría/nombre/estado) e `ix_incidencia_registrada_implemento` (unidades con incidencia abierta). Se incluye perfil k6 para medir 100 VU y objetivo p95 menor a 1,5 s (RNF-01); el plan debe revisarse con `EXPLAIN (ANALYZE, BUFFERS)` sobre datos representativos.

## Criterios de prueba

Pruebas PostgreSQL del repositorio con varias categorías y estados; pruebas HTTP de ambas rutas, acceso anónimo, filtros, paginación, respuesta vacía y ausencia de códigos/UUID; una unidad prestada, en mantenimiento, retirada o con incidencia abierta no aumenta la disponibilidad. La medición de carga debe usar el k6 incluido en `apps/backend/performance/availability.js`.

## Decisiones respecto a BE-01

BE-01 define el agregado público y el formato de datos, pero su ejemplo incluye grupos con cero disponibilidad. El criterio explícito de BE-08 pide `contenido: []` si no hay unidades prestables, así que se omiten esos grupos manteniendo el formato agregado y los metadatos BE-01. La ruta inglesa se agrega como alias por el contrato del issue; la ruta española permanece disponible.
