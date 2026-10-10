# Contexto de agentes — backend

Estas reglas aplican al trabajo dentro de `apps/backend/` y complementan el `AGENTS.md` de la raíz.

Cuando esta carpeta deje de ser una estructura vacía y se inicialice la aplicación backend, actualizar inmediatamente las referencias de estado en el `AGENTS.md` raíz y en `docs/agent-context/arquitectura-tecnica.md`. Mantener también sincronizados aquí los comandos, herramientas y convenciones que realmente existan.

## Arquitectura

- Usar Java/Spring Boot y PostgreSQL únicamente cuando una especificación autorice inicializar la aplicación.
- Separar, como mínimo conceptualmente, entrada REST, servicios de aplicación, dominio, persistencia y manejo transversal de errores.
- Los controladores traducen solicitudes y respuestas; no concentran reglas de negocio.
- Los servicios de aplicación coordinan casos de uso y límites transaccionales.
- El dominio expresa invariantes, estados y políticas del préstamo.
- Los repositorios abstraen persistencia; ninguna capa de presentación accede directamente a la base de datos.
- La autenticación y autorización del backend son la autoridad real de seguridad. Las restricciones del frontend no son suficientes.

## Convenciones de diseño

- Preferir nombres de dominio claros y consistentes con el C4: `Prestamo`, `Devolucion`, `Incidencia`, `ImplementoDeportivo`, `ActualizadorDisponibilidad` y políticas relacionadas.
- Usar DTO para contratos de entrada y salida; no exponer entidades de persistencia sin una decisión explícita de la especificación.
- Representar errores de validación, autorización, conflicto e infraestructura con respuestas estables y comprensibles.
- Mantener las operaciones que modifican préstamo, devolución, incidencia e inventario en una coordinación transaccional coherente.
- Proteger la exclusividad de una unidad física también con mecanismos apropiados de base de datos cuando corresponda.

## Patrones y SOLID

- `State`: estados y transiciones válidas de una unidad física.
- `Chain of Responsibility`: validaciones encadenadas de elegibilidad de un préstamo.
- `Strategy`: políticas alternativas, especialmente el cálculo de sanciones.
- `Facade`: entrada simplificada a un caso de uso frente a múltiples subsistemas.
- `Mediator`: coordinación transversal entre préstamos, devoluciones, incidencias e inventario.

No introducir un patrón solo por cumplir una lista. Consultar `docs/agent-context/patrones-solid.md` antes de extender el diseño.

