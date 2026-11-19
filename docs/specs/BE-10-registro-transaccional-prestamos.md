# BE-10 — Registro transaccional de préstamos

## Trazabilidad y dependencias

Implementa HU-09, HU-12, RF-11 y RF-13, además de RN-01, RN-02, RN-04, RN-05, RN-06 y RNF-05 de BE-01. Depende de BE-03 a BE-09.

## Contrato

- `POST /api/v1/prestamos` es el contrato canónico BE-01; `POST /api/v1/loans` es alias de BE-10.
- El cuerpo acepta `usuarioId`/`implementoIds` y sus alias `userId`/`unitIds`. Requiere solicitante y de 1 a 4 UUIDs únicos. No acepta fechas ni observaciones: BE-01 define que el servidor fija las fechas y no define observación de entrega.
- Solo PRESTADOR autenticado. El usuario autenticado se registra como operador.
- Responde 201 con el contrato detallado de BE-01 y `Location` del recurso canónico.

## Consistencia transaccional

Una Facade transaccional bloquea primero la fila del solicitante para serializar solicitudes que podrían exceder RN-01; después bloquea las unidades por UUID en orden estable y comprueba que todas existen. Reejecuta la cadena BE-09 dentro de esa transacción; calcula entrega `Clock.instant()` y vencimiento a 24 horas; crea préstamo y detalles; pasa cada unidad a PRESTADO y registra auditoría. Cualquier fallo revierte toda la operación.

El bloqueo pesimista serializa peticiones por unidad. El índice único parcial `uq_detalle_prestamo_implemento_pendiente` es la garantía final RN-06 y produce conflicto 409 ante una carrera. El evento `PRESTAMO` se agrega al historial de transiciones mediante migración aditiva.

## Pruebas

Unitarias de facade/validación y pruebas PostgreSQL/Testcontainers de creación, rollback, restricción única y concurrencia real.
