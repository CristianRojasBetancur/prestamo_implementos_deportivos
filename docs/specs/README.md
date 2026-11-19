# Especificaciones SDD

Esta carpeta está reservada para las especificaciones de funcionalidades futuras.

Cada funcionalidad debe tener una especificación identificable antes de implementar código. La especificación es la autoridad funcional y prevalece sobre el contexto técnico cuando define explícitamente un comportamiento.

Los documentos de contexto no deben utilizarse como sustituto de una especificación.

## Índice

| ID | Especificación | Contenedor | Estado |
|---|---|---|---|
| FE-01 | [Aplicación web del MVP](FE-01-frontend-mvp.md) | Aplicación web | Implementada (modo demostración) |
| BE-01 | [API backend del MVP](BE-01-api-backend-mvp.md) | API backend | Base contractual; implementación por módulos |
| BE-02 | [Gestión de prestadores](BE-02-gestion-prestadores.md) | API backend | Implementación en esta rama |
| BE-13 | [Sanciones por retraso mediante Strategy](BE-13-sanciones-por-retraso.md) | API backend | Implementada |

BE-01 define los contratos comunes de `apps/backend/`. Las especificaciones de
módulo concretan cada caso de uso y documentan de forma explícita cualquier
desviación de esos contratos, como las rutas `/providers` requeridas por BE-02.
