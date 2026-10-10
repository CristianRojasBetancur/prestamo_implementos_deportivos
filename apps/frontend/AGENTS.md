# Contexto de agentes — frontend

Estas reglas aplican al trabajo dentro de `apps/frontend/` y complementan el `AGENTS.md` de la raíz.

Cuando esta carpeta deje de ser una estructura vacía y se inicialice la aplicación frontend, actualizar inmediatamente las referencias de estado en el `AGENTS.md` raíz y en `docs/agent-context/arquitectura-tecnica.md`. Mantener también sincronizados aquí los comandos, herramientas y convenciones que realmente existan.

## Arquitectura

- Usar React/Vite únicamente cuando una especificación autorice inicializar la aplicación.
- Organizar la interfaz por flujo funcional: autenticación, disponibilidad, prestadores, inventario, préstamos y devoluciones/incidencias.
- Mantener componentes de presentación enfocados en renderizado e interacción; las reglas de negocio pertenecen al backend.
- Centralizar la comunicación HTTP en un cliente y adaptadores tipados por recurso o caso de uso.
- Mantener separados estado de sesión, protección de rutas, vistas, formularios, tablas y servicios de API.
- La protección de rutas mejora la navegación, pero nunca reemplaza la autorización del backend.

## Convenciones

- Usar nombres coherentes con los componentes C4 y con la especificación de la funcionalidad.
- Tipar contratos de entrada y salida; normalizar errores en un punto común.
- Evitar duplicar lógica de autorización, serialización o manejo de errores en cada vista.
- Mantener la interfaz responsive y accesible cuando la especificación no indique otra cosa.
- Agregar pruebas de componentes y flujos para los criterios de aceptación definidos en SDD.

