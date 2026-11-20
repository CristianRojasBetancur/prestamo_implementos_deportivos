# BE-18 — Rendimiento, observabilidad y seguridad del backend

| Campo | Valor |
|---|---|
| Identificador | `BE-18` |
| Estado | Implementación en esta rama |
| Contenedor | API backend |
| Requisitos relacionados | RNF-01, RNF-02, RNF-03, RNF-05 |
| Dependencias | BE-02, BE-03, BE-04, BE-05, BE-08, BE-10, BE-15 y BE-17 |

## Objetivo

Proporcionar evidencia técnica reproducible y controles mínimos de rendimiento,
observabilidad, seguridad e integridad para el backend del MVP sin añadir
servicios externos ni ampliar el alcance funcional.

## Comportamiento y límites

- Las consultas críticas se miden con hasta 100 usuarios virtuales concurrentes.
- El umbral automatizado es p95 menor o igual a 1.500 ms por etiqueta de ruta y
  menos de 1 % de respuestas fallidas.
- La paginación limita las respuestas a 100 elementos por página; las búsquedas
  textuales se limitan a 80 caracteres y los cuerpos mantienen los límites de
  sus DTO.
- Las operaciones de préstamo, devolución e incidencia conservan un único
  límite transaccional y el Mediator de disponibilidad no abre transacciones
  independientes.
- Liveness representa la aplicación; readiness representa aplicación más
  PostgreSQL. Health no expone detalles internos.
- Los logs y errores no contienen contraseñas, tokens, hashes, payloads
  completos, identificaciones ni mensajes SQL.
- La autenticación JWT es stateless, la autorización es deny-by-default y los
  roles se validan en la cadena de seguridad y en los controladores.

## Evidencia reproducible

- `apps/backend/performance/critical-api.js` ejecuta las cinco familias de rutas
  críticas con 100 VU.
- `apps/backend/performance/availability.js` conserva el escenario público de
  disponibilidad.
- `apps/backend/performance/explain.sql` contiene planes base para disponibilidad,
  préstamos, devoluciones e incidencias.
- `apps/backend/performance/README.md` define configuración, dataset mínimo,
  variables de entorno, comandos y formato de resultados.
- Las migraciones Flyway mantienen índices compuestos para filtros por estado,
  alcance, tipo y fecha en los listados operativos.

## Disponibilidad

El endpoint de health y las sondas de liveness/readiness permiten distinguir
proceso disponible de dependencia PostgreSQL disponible. La medición local no
demuestra ni garantiza 99,5 % mensual: esa afirmación requiere infraestructura
de despliegue, redundancia, monitoreo y recuperación que están fuera de este
issue.

## Verificación

- Suite Maven con PostgreSQL de Testcontainers.
- Pruebas de validación de límites, cabeceras, correlación, autorización y
  ausencia de secretos en errores.
- Prueba de lote de elegibilidad para evitar consultas por unidad y reducir el
  riesgo N+1.
- Ejecución de k6 y `EXPLAIN (ANALYZE, BUFFERS)` sobre el dataset documentado.
- Checklist OWASP aplicable en
  `docs/security-checklist-backend.md`.
