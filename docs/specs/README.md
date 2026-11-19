# Especificaciones SDD

Esta carpeta está reservada para las especificaciones de funcionalidades futuras.

Cada funcionalidad debe tener una especificación identificable antes de implementar código. La especificación es la autoridad funcional y prevalece sobre el contexto técnico cuando define explícitamente un comportamiento.

Los documentos de contexto no deben utilizarse como sustituto de una especificación.

## Índice

| ID | Especificación | Contenedor | Estado |
|---|---|---|---|
| FE-01 | [Aplicación web del MVP](FE-01-frontend-mvp.md) | Aplicación web | Implementada (modo demostración) |
| BE-01 | [API backend del MVP](BE-01-api-backend-mvp.md) | API backend | Propuesta para revisión del equipo |

BE-01 gobierna toda implementación en `apps/backend/`: contratos HTTP/JSON, errores, reglas de negocio, trazabilidad y decisiones sobre ambigüedades del documento fuente.
