# Instrucciones para la documentación

## Organización

- `source/` contiene los documentos originales del proyecto y debe conservar su contenido.
- `modelo-c4/` contiene la estructura organizada del entregable del modelo C4, con su documento, diagramas exportados e imágenes de apoyo.
- `agent-context/` contiene síntesis operativas para orientar a los agentes.
- `specs/` contiene especificaciones SDD de funcionalidades. Una especificación no debe confundirse con documentación de contexto.

Las nuevas decisiones arquitectónicas deben actualizar el contexto correspondiente o una especificación, según su naturaleza. No modificar silenciosamente una fuente original para resolver una decisión de implementación.

## Mantenimiento del contexto

Los documentos de `agent-context/` y las instrucciones `AGENTS.md` son documentación viva. Deben actualizarse cuando el repositorio cambie de forma material: al inicializar backend o frontend, incorporar una tecnología, modificar la arquitectura C4, añadir una integración, cambiar convenciones, adoptar o retirar un patrón, o alterar el alcance del producto.

Después de cada cambio relevante, el agente debe buscar afirmaciones temporales o de estado —por ejemplo, que una aplicación aún no está inicializada— y corregirlas en todos los archivos afectados. Los documentos de `source/` se conservan sin reescritura; las actualizaciones de estado deben reflejarse en el contexto operativo y, cuando corresponda, en una especificación o decisión arquitectónica.

## Especificaciones SDD

Cada especificación debe dejar claros el objetivo, alcance, actores, comportamiento esperado, reglas, contratos, errores, criterios de aceptación y pruebas. Las funcionalidades deben implementarse únicamente contra una especificación identificable.
