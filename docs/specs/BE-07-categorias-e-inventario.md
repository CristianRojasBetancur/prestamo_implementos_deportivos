# BE-07 — Categorías e inventario de unidades físicas

| Estado | Implementado; integración pendiente de ejecución con Docker |
|---|---|
| Actores | `ADMIN`; consulta pública de categorías según BE-01 |
| Base | `docs/specs/BE-01-api-backend-mvp.md`, HU-05 a HU-07 |

## Objetivo y trazabilidad

Permitir que ADMIN gestione categorías y unidades físicas, y conserve estas unidades consultables durante todo su ciclo de vida. Trazabilidad: HU-05/CU-08/RF-07 (registro), HU-06/CU-09 y CU-10/RF-08 (consulta y actualización), HU-07/CU-11/RF-09 (estado, baja e historial), RF-10 (categorías), RN-04 (disponibilidad) de BE-01.

## Contrato

- Categorías: `GET /api/v1/categorias` (consulta pública), `POST /api/v1/categorias` y `PUT /api/v1/categorias/{id}` (ADMIN). No se ofrece borrado físico.
- Unidades: `POST /api/v1/inventory/units`, `GET /api/v1/inventory/units` (paginado), `GET /api/v1/inventory/units/{id}`, `PATCH /api/v1/inventory/units/{id}` y `PATCH /api/v1/inventory/units/{id}/state` (ADMIN). La ruta inglesa de inventario es la convención específica solicitada para BE-07; se conserva el vocabulario y formato de datos de BE-01.
- La búsqueda `q` compara código y nombre sin distinguir mayúsculas ni tildes; `categoriaId` y `estado` son filtros exactos. Paginación usa `pagina`, `tamano` y `orden` según BE-01.
- Alta admite estado inicial `DISPONIBLE` o `EN_MANTENIMIENTO`; se omite para usar `DISPONIBLE`. `PRESTADO` y `DADO_DE_BAJA` no son estados iniciales válidos.
- PATCH de datos modifica código, nombre, descripción y categoría. El estado solo cambia por el subrecurso `/state`, con motivo obligatorio.
- La respuesta incluye UUID, código, nombre, descripción, referencia de categoría, estado, elegibilidad para préstamo y fechas de creación/actualización. Nunca se exponen entidades JPA.
- Códigos duplicados producen `409 CONFLICTO_UNICIDAD`; referencias inexistentes `404`; categoría inactiva `422 CATEGORIA_INACTIVA`; datos inválidos `400`; transición no válida `409 TRANSICION_INVALIDA`.

## Reglas de dominio

- La categoría debe existir y estar activa al crear o cambiar categoría. Desactivarla no afecta sus unidades existentes; nombres de categoría son únicos sin distinguir mayúsculas.
- Estados persistidos siguen BE-01: `DISPONIBLE`, `PRESTADO`, `EN_MANTENIMIENTO`, `DADO_DE_BAJA`. Incidencias son recursos separados (`REGISTRADA`, `RESUELTA`), no estados adicionales de la unidad.
- Solo `DISPONIBLE` es seleccionable para préstamo. `EN_MANTENIMIENTO` y `DADO_DE_BAJA` no lo son.
- ADMIN puede enviar `DISPONIBLE -> EN_MANTENIMIENTO|DADO_DE_BAJA` y `EN_MANTENIMIENTO -> DISPONIBLE|DADO_DE_BAJA`. `PRESTADO` solo sale por el flujo de devolución/incidencia, nunca por cambio administrativo directo. La política de dominio admite los eventos de devolución e incidencia para los módulos que los implementen.
- Una unidad dada de baja es terminal. Ninguna operación de este módulo elimina unidades ni categorías que deban conservar historial.

## Pruebas de aceptación

Verificar restricciones de transición en dominio; registro/consulta/filtros/paginación/actualización/estado en servicios y web; restricciones únicas y FK en persistencia; ADMIN frente a USER, PRESTADOR y anónimo; persistencia de la unidad retirada y ausencia de elegibilidad para préstamo.

## Desviaciones explícitas de BE-01

BE-01 propone `POST/GET/PUT /api/v1/implementos` y `PATCH /api/v1/implementos/{id}/estado`. BE-07 conserva sus casos de uso y política, pero usa `/api/v1/inventory/units` y PATCH para los datos porque este issue solicita explícitamente dichas rutas y verbo. Las categorías y enums mantienen las rutas/valores existentes de BE-01. No se añade un estado de unidad para incidencias, porque BE-01 separa el estado de `Incidencia` del estado físico de la unidad.
