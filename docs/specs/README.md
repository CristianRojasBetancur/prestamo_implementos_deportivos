# Especificaciones SDD

Esta carpeta está reservada para las especificaciones de funcionalidades futuras.

Cada funcionalidad debe tener una especificación identificable antes de implementar código. La especificación es la autoridad funcional y prevalece sobre el contexto técnico cuando define explícitamente un comportamiento.

Los documentos de contexto no deben utilizarse como sustituto de una especificación.

## Índice

| ID | Especificación | Contenedor | Estado |
|---|---|---|---|
| FE-01 | [Aplicación web del MVP](FE-01-frontend-mvp.md) | Aplicación web | Implementada (modo demostración) |
| BE-01 | [API backend del MVP](BE-01-api-backend-mvp.md) | API backend | Propuesta para revisión del equipo |
| BE-01-A | [Contratos comunes y formato de errores](BE-01-A-contratos-y-errores.md) | API backend | Implementado |
| BE-07 | [Categorías e inventario de unidades físicas](BE-07-categorias-e-inventario.md) | API backend | Implementado; integración pendiente de ejecución con Docker |
| BE-08 | [Consulta pública de disponibilidad](BE-08-consulta-disponibilidad.md) | API backend | Implementado; integración y medición RNF-01 pendientes del entorno |
| BE-09 | [Elegibilidad y situación de préstamo del usuario](BE-09-elegibilidad-y-situacion-usuario.md) | API backend | En implementación |

BE-01 gobierna toda implementación en `apps/backend/`: contratos HTTP/JSON, errores, reglas de negocio, trazabilidad y decisiones sobre ambigüedades del documento fuente. Una especificación de módulo concreta sus casos de uso y documenta explícitamente cualquier desviación de contrato.
