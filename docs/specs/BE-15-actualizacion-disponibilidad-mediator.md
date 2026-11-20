# BE-15 — Actualización de disponibilidad mediante Mediator

| Campo | Valor |
|---|---|
| Identificador | `BE-15` |
| Contenedor | API backend |
| Relacionado | BE-07, BE-10, BE-12, BE-13 y BE-14 |
| Estado | Implementada |

## Objetivo

Coordinar en un único componente de aplicación los cambios de disponibilidad
producidos por préstamos, devoluciones e incidencias, manteniendo consistentes
la unidad física, el detalle del préstamo, los registros operativos y el
historial de estados.

## Reglas

- Un préstamo confirmado cambia cada unidad de `DISPONIBLE` a `PRESTADO`.
- Una devolución `BUENO` cambia únicamente su unidad de `PRESTADO` a `DISPONIBLE`.
- Una devolución `CON_NOVEDAD` deja la unidad en `EN_MANTENIMIENTO` y exige una incidencia.
- Una incidencia independiente de `PERDIDA` o `ROBO` deja la unidad en `DADO_DE_BAJA`.
- Las unidades en mantenimiento, dadas de baja o con incidencia pendiente no se liberan automáticamente.
- Un préstamo permanece activo mientras exista un detalle `PENDIENTE_DEVOLUCION`.
- Solo pasa a `FINALIZADO` cuando todos sus detalles fueron devueltos o resueltos mediante incidencia.
- Una operación parcial o fallida no confirma cambios parciales.
- Cada transición nueva registra unidad, estado anterior, estado nuevo, evento, motivo y operador.

## Diseño

`ActualizadorDisponibilidad` implementa los puertos `ReservaDisponibilidad`,
`LiberacionDisponibilidad`, `IncidenciaDisponibilidad` y `CierrePrestamo`.
Coordina unidades ya bloqueadas por el servicio de aplicación y participa en la
transacción existente; no abre una transacción independiente ni decide
elegibilidad, sanciones o reglas de transición.

`EstadoImplemento` conserva la validación State. `PoliticaCierrePrestamo`
conserva la regla global de cierre. `DevolucionServicio` conserva el cálculo de
sanciones y `IncidenciaServicio` conserva la creación y resolución de incidencias.

## Persistencia y errores

La migración V9 agrega `operador_id` al historial de estados. Los registros
históricos anteriores pueden carecer de operador; toda transición nueva,
incluidas las administrativas, debe proporcionarlo. Los conflictos de estado mantienen `409 TRANSICION_INVALIDA`,
`PRESTAMO_FINALIZADO` o `UNIDAD_YA_RESUELTA` según corresponda.

## Verificación

Las pruebas cubren la matriz de transiciones, auditoría del operador,
devoluciones parciales y completas, incidencias, rollback transaccional,
restricción de liberación administrativa y carreras sobre una misma unidad.
