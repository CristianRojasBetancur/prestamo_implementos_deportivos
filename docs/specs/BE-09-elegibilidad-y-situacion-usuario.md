# BE-09 — Elegibilidad y situación de préstamo del usuario

## Trazabilidad

Implementa RF-12, HU-11, validaciones previas de HU-09, RN-01, RN-02, RN-04 y RN-05 de BE-01. Depende de BE-03, BE-04, BE-05, BE-07 y BE-08.

## Contratos

- `GET /api/v1/usuarios/{id}/situacion` (canónico BE-01) y alias `GET /api/v1/users/{userId}/loan-situation` (BE-09), solo PRESTADOR.
- Solo un usuario existente con rol USER es consultable; en otro caso responde 404.
- El GET responde 200 incluso si no es elegible y no modifica datos. `habilitado` y `motivos` expresan las reglas incumplidas. No incluye credenciales.
- Devuelve identidad básica, unidades activas, máximo (4), capacidad restante, préstamos activos/vencidos y sanción vigente.

## Política de validación

La cadena evalúa en orden: solicitud (1–4 IDs únicos), solicitante USER activo, suspensión, vencidos, límite de cuatro unidades pendientes, fechas (inicio no futuro y límite exactamente inicio + 24 horas), y unidades existentes/disponibles sin incidencia REGISTRADA. En la operación de validación de préstamo, el servidor genera inicio y límite como `ahora` y `ahora + 24 h`, según RN-02; el validador rechaza datos temporales inconsistentes cuando es usado por otro caso de aplicación. La consulta informativa omite las reglas que requieren una solicitud concreta. Los errores de elegibilidad conservan los códigos de BE-01 y el conflicto de disponibilidad identifica cada unidad.

La operación de aplicación `validarSolicitudParaPrestamo` ejecuta la misma cadena y es el punto de integración para el registro de préstamos de BE-10. Este cambio no crea el endpoint de registro de préstamos.

## Verificación y límites

Pruebas unitarias por validador, composición/orden, servicio, endpoint y autorización. Fechas usan `Clock`. El repositorio aún no incluye el caso de uso de creación del préstamo, por lo que la invocación transaccional final deberá integrarse al implementar BE-10.
