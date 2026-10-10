# Contexto funcional del proyecto

Este documento resume el contexto real del Sistema de Préstamo de Implementos Deportivos. No es una especificación de funcionalidades. Las especificaciones SDD de `docs/specs/` tienen precedencia sobre este contexto.

El contexto es vivo y debe actualizarse cuando cambien el alcance aprobado, los actores, las reglas de negocio, los sistemas involucrados o el estado real del producto. No deben mantenerse descripciones obsoletas después de que una capacidad haya sido implementada o descartada.

## Problema y objetivo

El préstamo de implementos deportivos se realiza principalmente de forma manual. Esto provoca demoras, desconocimiento de la disponibilidad, pérdida de trazabilidad, dificultad para controlar fechas límite y falta de verificación consistente de daños, pérdidas o robos.

El objetivo es automatizar y dar trazabilidad al ciclo de vida del préstamo: autenticación, consulta de disponibilidad, validación de elegibilidad, entrega, devolución e incidencias.

## Actores

| Actor | Responsabilidad principal |
|---|---|
| Usuario | Consulta implementos disponibles por categoría y solicita atención para obtenerlos. |
| Administrador | Administra prestadores, categorías e inventario. |
| Prestador | Verifica la situación del usuario, registra préstamos, devoluciones e incidencias y verifica el estado físico de los implementos. |

## Alcance del MVP

- Autenticación y control de acceso por rol.
- Gestión de prestadores: registrar, consultar, actualizar y desactivar.
- Gestión de inventario: categorías, unidades físicas, estados, disponibilidad y retiro.
- Consulta de disponibilidad por categoría.
- Registro y consulta de préstamos.
- Devoluciones parciales o totales.
- Incidencias por daño, pérdida, robo u otra novedad.
- Historial de operaciones y sanciones derivadas de entregas tardías.

## Fuera de alcance

No forman parte del contexto actual: ERP, mapas, geolocalización, notificaciones externas, reservas anticipadas independientes, pagos, cobros, gestión financiera de pérdidas, analítica avanzada, recomendaciones y sincronización con otros inventarios o sistemas académicos.

## Reglas de negocio críticas

- `RN-01`: un usuario puede tener como máximo cuatro implementos en préstamo.
- `RN-02`: un préstamo regular dura como máximo 24 horas desde la entrega.
- `RN-03`: una entrega tardía sin justificación genera una suspensión equivalente al doble del retraso, con mínimo de dos días.
- `RN-04`: implementos en mantenimiento o dados de baja no pueden reservarse ni prestarse.
- `RN-05`: un usuario habilitado no puede recibir implementos si tiene préstamos vencidos pendientes.
- `RN-06`: una unidad física no puede pertenecer simultáneamente a dos préstamos activos.
- `RN-07`: el préstamo finaliza cuando todos sus implementos fueron devueltos o resueltos mediante una incidencia.

## Trazabilidad esperada

Cada funcionalidad futura debe poder relacionarse con un actor, una especificación SDD, uno o más casos de uso, reglas aplicables, componentes responsables y pruebas. El contexto no autoriza implementar una funcionalidad por sí mismo.

## Fuentes

La información se consolidó a partir de `source/contexto_proyecto_sistemas_de_informacion.pdf`, `source/modelo_c4.md`, `source/seleccion_de_patrones_de_diseno.md`, `source/PLAN_IMPLEMENTACION.md` y los documentos de apoyo ubicados en `source/`.

