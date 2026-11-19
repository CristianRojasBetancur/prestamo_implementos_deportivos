# BE-13 — Aplicar sanciones por retraso mediante Strategy

## Trazabilidad

Implementa RN-03 dentro de CU-16 (registro de devolución) y se integra con
BE-09 (elegibilidad), BE-10 (transacciones), BE-11 (historial) y BE-12
(devoluciones parciales y totales). La fuente funcional original es el issue
14 del repositorio.

## Reglas

- El servidor compara `fechaDevolucion` con `fechaLimiteDevolucion` usando el
  `Clock` inyectado. Los cálculos se realizan sobre `Instant`; la zona
  `app.zona-horaria` solo se aplica al serializar respuestas.
- Una devolución dentro del plazo, o exactamente en la fecha límite, no crea
  sanción.
- Una devolución tardía sin justificación crea una sanción con:
  - `minutosRetraso = max(1, minutos entre fecha límite y devolución)`;
  - duración `max(2 días, 2 × minutosRetraso)`;
  - estado inicial `ACTIVA` y motivo de devolución tardía sin justificación.
- Si `retrasoJustificado=true`, `justificacion` es obligatoria y no se crea la
  sanción automática de RN-03.
- Las sanciones se conservan como historial. `usuario.suspendido_hasta` se
  actualiza con la fecha más lejana entre la existente y la nueva.
- La política se ejecuta dentro de la misma transacción de devolución. Un
  error revierte devolución, incidencia, estados de inventario, préstamo,
  sanción y suspensión.
- No se implementan cobros, multas económicas ni notificaciones externas.

## Contrato e integración

No se agregan rutas. El cuerpo existente de `POST /api/v1/prestamos/{id}/devoluciones`
ya acepta `retrasoJustificado` y `justificacion`; la respuesta de devolución
expone `sancion` cuando se aplica. El detalle autorizado de la devolución es el
historial consultable de la sanción.

El servicio depende de la abstracción `PoliticaSancion`, implementada por las
strategies `SinSancionPorRetrasoJustificado` y
`SancionPorRetrasoSinJustificacion`. La selección se realiza por aplicabilidad,
en orden Spring, y permite sustituir políticas en pruebas.

## Verificación

- Unitarias con `Instant` fijo para plazo, límite mínimo, doble del retraso y
  justificación.
- Integración PostgreSQL/Testcontainers para persistencia, elegibilidad,
  suspensión, historial y rollback transaccional.
- Pruebas existentes de devoluciones parciales, incidencias, autorización y
  esquema deben permanecer verdes.
