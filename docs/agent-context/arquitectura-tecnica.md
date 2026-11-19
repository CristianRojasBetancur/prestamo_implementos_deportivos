# Contexto técnico y arquitectura

Este documento orienta decisiones de implementación. No crea APIs, aplicaciones ni contratos funcionales. La especificación SDD de cada funcionalidad es la autoridad para el comportamiento concreto.

Este es un documento vivo: debe actualizarse cuando se inicialicen los contenedores backend o frontend, cambien sus tecnologías, se incorporen componentes, se modifiquen límites transaccionales o aparezcan integraciones autorizadas por una especificación.

## Arquitectura de referencia

El modelo C4 define tres contenedores:

| Contenedor | Responsabilidad | Tecnología de referencia |
|---|---|---|
| Aplicación web MVP | Presentación, interacción, sesión y navegación según rol. | React/Vite |
| API backend | Autenticación, autorización, reglas de negocio, coordinación de casos de uso y transacciones. | Java/Spring Boot |
| Base de datos | Persistencia íntegra de usuarios, inventario, préstamos, devoluciones e incidencias. | PostgreSQL |

La aplicación web se comunica con la API mediante HTTP/JSON sobre HTTPS. La web nunca accede directamente a PostgreSQL; el backend es la autoridad para autorización y reglas de negocio.

## Estado de inicialización

`apps/frontend/` está inicializado con React, Vite y TypeScript. El alcance de
interfaz implementado inicialmente está descrito en
`docs/specs/FE-01-frontend-mvp.md`. El cliente HTTP centraliza la URL base y los
errores; mientras no consuma la API, las pantallas presentan estados vacíos y
no confirman operaciones.

`apps/backend/` está inicializado como aplicación Spring Boot 4.1 (Java 21,
Maven) según `docs/specs/BE-01-api-backend-mvp.md`, que es el contrato de la
API. Contiene la base técnica: perfiles `local`, `test` y `prod`; migraciones
Flyway; PostgreSQL 17 local con Docker Compose y Testcontainers en pruebas;
seguridad stateless deny-by-default; CORS por variable de entorno; y
`/actuator/health` público. El esquema inicial del MVP (9 tablas, migración V2), sus
entidades JPA y repositorios están creados y documentados en
`docs/agent-context/modelo-datos.md`. La capa transversal de contratos está
implementada: DTO de todos los casos de uso, validación, formato de error
`application/problem+json` con catálogo de códigos, correlación
`X-Correlation-Id`, logs con MDC y OpenAPI (`docs/specs/BE-01-A-contratos-y-errores.md`).
La autenticación JWT (login, sesión actual y cierre de sesión con revocación) y la
autorización por rol están implementadas (BE-01 §5, D-16 y D-17). BE-07 implementa
la administración de categorías y unidades físicas, con paginación, filtros,
transiciones de estado auditadas y baja lógica. BE-08 agrega la consulta pública
agregada y paginada de disponibilidad, sin códigos ni UUID de unidades. Los módulos de préstamos,
devoluciones e incidencias aún no tienen sus casos de uso REST implementados en
esta rama. Los comandos están en `apps/backend/README.md`.

## Componentes de referencia

### Frontend

- `AutenticacionUI`: inicio/cierre de sesión y estado autenticado.
- `CatalogoDisponibilidadUI`: consulta y filtrado de disponibilidad.
- `GestionPrestadoresUI`: administración de prestadores.
- `GestionInventarioUI`: administración de categorías, unidades y estados.
- `OperacionPrestamosUI`: consulta de situación, validación previa y registro.
- `OperacionDevolucionesUI`: devoluciones, condiciones, incidencias e historial.
- `ClienteApi`: comunicación HTTP, serialización y errores comunes.
- `ProteccionRutas`: restricción de navegación según sesión y rol.

### Backend

- `AutenticacionAutorizacion`.
- `GestionPrestadores`.
- `GestionInventario`.
- `GestionPrestamos`.
- `GestionDevoluciones`.
- `GestionIncidencias`.
- `ActualizacionDisponibilidad`.
- `Persistencia`.

Los componentes representan responsabilidades significativas, no entidades aisladas. Las entidades, DTO, repositorios, controladores y servicios son elementos de código que materializan esos componentes.

## Límites de responsabilidad

- Controladores: traducen contratos externos y delegan.
- Servicios de aplicación: coordinan casos de uso.
- Dominio: protege invariantes, estados y políticas.
- Repositorios/adaptadores: encapsulan persistencia.
- Frontend: presenta, captura y consulta; no es autoridad de seguridad ni de negocio.
- Base de datos: persiste y ayuda a garantizar integridad; no reemplaza la lógica de aplicación.

## Consistencia y transacciones

Registrar un préstamo debe validar usuario y unidades, guardar el préstamo y marcar las unidades como no disponibles dentro de una coordinación transaccional coherente.

Registrar una devolución debe guardar la devolución, registrar una incidencia si aplica, actualizar el estado de la unidad, aplicar la política de sanción y evaluar el cierre del préstamo dentro del mismo límite transaccional cuando corresponda.

La protección contra préstamos simultáneos debe complementarse con restricciones, bloqueos o mecanismos equivalentes de PostgreSQL definidos por la futura implementación y la especificación.

## Principios de evolución

- Mantener trazabilidad `actor → contenedor → componente → código → prueba`.
- No introducir contenedores para sistemas externos excluidos del MVP.
- No convertir cada entidad en un componente arquitectónico.
- Preferir interfaces en los límites que requieran sustitución o pruebas.
- Mantener contratos explícitos entre frontend y backend.
- Registrar las decisiones que modifiquen el modelo C4 o los patrones aprobados.

