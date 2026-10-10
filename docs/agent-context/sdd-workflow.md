# Flujo de desarrollo guiado por especificaciones

El proyecto se desarrollará mediante SDD (Specification-Driven Development). El contexto arquitectónico orienta la solución, pero la especificación de cada funcionalidad manda sobre el comportamiento que debe implementarse.

El flujo también incluye mantener actualizado el contexto: si una implementación cambia la arquitectura, las convenciones o el estado del monorepo, el agente debe actualizar los `AGENTS.md` y documentos de `docs/agent-context/` afectados antes de cerrar la tarea.

## Ciclo obligatorio

1. **Localizar la especificación:** identificar el archivo SDD aplicable en `docs/specs/`.
2. **Validar el contrato:** revisar objetivo, alcance, actores, reglas, entradas, salidas, errores, criterios de aceptación y restricciones.
3. **Detectar contradicciones:** comparar la especificación con `AGENTS.md` y `docs/agent-context/`. La especificación prevalece para el comportamiento funcional; las desviaciones arquitectónicas deben quedar documentadas.
4. **Diseñar:** mapear la funcionalidad a contenedores, componentes, dominio, contratos y persistencia sin inventar capacidades adicionales.
5. **Implementar:** modificar únicamente el alcance autorizado, respetando las instrucciones del directorio y los principios SOLID.
6. **Probar:** cubrir criterios de aceptación, reglas de negocio, errores, autorización, consistencia y regresiones relevantes.
7. **Verificar contra la especificación:** revisar cada criterio y señalar explícitamente lo que no pudo cumplirse.

## Contenido mínimo de una especificación futura

- Identificador y objetivo.
- Alcance y exclusiones.
- Actores y permisos.
- Flujo principal y flujos alternos.
- Reglas de negocio.
- Contratos de entrada y salida, si aplica.
- Errores y estados límite.
- Criterios de aceptación verificables.
- Estrategia de pruebas.
- Impacto arquitectónico o migraciones, si aplica.

## Regla contra la invención

Si la especificación no define un dato, estado, endpoint, pantalla, regla o integración, el agente no debe inventarlo como parte de la funcionalidad. Debe mantener la solución mínima o registrar la necesidad de ampliar la especificación.

