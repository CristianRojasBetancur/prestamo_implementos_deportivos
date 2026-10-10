# Instrucciones para agentes

## Autoridad y orden de lectura

Para desarrollar una funcionalidad, el agente debe respetar este orden:

1. La especificación SDD de la funcionalidad, ubicada en `docs/specs/`, es la autoridad funcional principal.
2. Las instrucciones de este archivo y del `AGENTS.md` del directorio donde se trabaje definen el proceso y las convenciones técnicas.
3. Los documentos de `docs/agent-context/` aportan contexto arquitectónico, técnico y de diseño.
4. Los documentos de `docs/source/` son fuentes de referencia del proyecto y no sustituyen una especificación funcional.

Si una especificación contradice una decisión contextual, se implementa la especificación y se documenta la desviación cuando sea relevante. El agente no debe inventar requisitos para completar vacíos de una especificación: debe señalar la ambigüedad o proponer una decisión explícita en el documento SDD.

## SDD obligatorio

Toda funcionalidad nueva o cambio de comportamiento debe trabajarse mediante SDD (Specification-Driven Development):

- leer y validar la especificación antes de modificar código;
- identificar alcance, actores, reglas, contratos, criterios de aceptación y casos de error;
- diseñar la solución respetando la arquitectura y los límites del proyecto;
- implementar únicamente lo especificado;
- agregar o actualizar pruebas para los criterios de aceptación;
- verificar la implementación contra la especificación y reportar cualquier desviación.

No se deben crear APIs, pantallas, entidades o integraciones que no estén respaldadas por una especificación vigente.

## Contexto del proyecto

- Es un sistema web para consultar disponibilidad y gestionar el ciclo de vida de préstamos de implementos deportivos.
- El MVP contempla autenticación, autorización por rol, prestadores, inventario, préstamos, devoluciones e incidencias.
- Los actores son Usuario, Administrador y Prestador.
- La arquitectura de referencia contiene aplicación web, API backend y PostgreSQL.
- Las tecnologías de referencia son React/Vite, Java/Spring Boot y PostgreSQL. Actualmente ninguna aplicación está inicializada.
- No deben agregarse ERP, mapas, geolocalización, notificaciones externas, pagos, reservas independientes ni analítica avanzada salvo que una especificación cambie explícitamente el alcance.

## Reglas técnicas generales

- Mantener responsabilidades pequeñas, dependencias explícitas y límites claros entre presentación, aplicación, dominio y persistencia.
- Las reglas de negocio deben vivir en el backend/dominio, no en el frontend ni únicamente en controladores.
- El frontend no accede directamente a PostgreSQL.
- Las operaciones de préstamo, devolución e incidencia deben preservar la consistencia del inventario dentro de límites transaccionales apropiados.
- Aplicar SOLID y los patrones aprobados solo cuando exista una necesidad real descrita en el contexto o en la especificación.
- Evitar sobreingeniería, duplicación de reglas y abstracciones sin consumidor.
- Mantener trazabilidad entre especificación, casos de uso, componentes, código y pruebas.

## Estructura del monorepo

- `apps/backend/`: futura aplicación backend; todavía no contiene una aplicación inicializada.
- `apps/frontend/`: futura aplicación frontend; todavía no contiene una aplicación inicializada.
- `docs/agent-context/`: contexto operativo para agentes.
- `docs/source/`: documentos originales aportados por el proyecto.
- `docs/specs/`: especificaciones SDD de funcionalidades futuras.
- `.codebase-memory/`: artefactos de indexación; no tratarlo como documentación funcional.

## Mantenimiento obligatorio del contexto

Los archivos de contexto deben mantenerse sincronizados con el estado real del repositorio. Antes de terminar una tarea, el agente debe revisar si cambió alguno de estos aspectos y actualizar la documentación correspondiente cuando sea necesario:

- existencia o estado de `apps/backend/` y `apps/frontend/`;
- tecnologías, herramientas, dependencias o comandos disponibles;
- arquitectura, contenedores, componentes, límites o integraciones;
- convenciones de código, pruebas, despliegue o persistencia;
- patrones de diseño, principios técnicos o decisiones adoptadas;
- alcance, exclusiones o trazabilidad del proyecto.

Por ejemplo, cuando se inicialice la aplicación backend o frontend, debe eliminarse o reemplazarse la descripción de que todavía no está inicializada y deben actualizarse los `AGENTS.md` y documentos de `docs/agent-context/` que dependan de ese estado. El contexto no debe conservar afirmaciones obsoletas solo por mantener el texto histórico; los documentos originales de `docs/source/` sí deben preservarse como evidencia de referencia.

