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

`apps/frontend/` está inicializado con React, Vite y TypeScript. FE-01 describe
la interfaz MVP y FE-02 gobierna su integración con la API. El cliente HTTP y
los adaptadores tipados centralizan URL base, sesión, correlación y errores; las
vistas no contienen fixtures ni confirman operaciones sin respuesta exitosa.

`apps/backend/` está inicializado como aplicación Spring Boot 4.1 (Java 21,
Maven) según `docs/specs/BE-01-api-backend-mvp.md`, que es el contrato de la
API. Contiene la base técnica: perfiles `local`, `test` y `prod`; migraciones
Flyway; PostgreSQL 17 local con Docker Compose y Testcontainers en pruebas;
seguridad stateless deny-by-default; CORS por variable de entorno; y
`/actuator/health` público. El backend implementa autenticación JWT, gestión de
prestadores, elegibilidad, préstamos, consultas, devoluciones BE-12 y sanciones
por retraso BE-13, incluyendo la coordinación transaccional con inventario,
incidencias y sanciones. Los
comandos están en `apps/backend/README.md`.

## Componentes de referencia

### Frontend

- `AutenticacionUI`: inicio/cierre de sesión y estado autenticado.
- `CatalogoDisponibilidadUI`: consulta y filtrado de disponibilidad.
- `GestionPrestadoresUI`: administración de prestadores.
- `AltaSolicitantesUI`: registro operativo de solicitantes desde el asistente de
  préstamos, usando únicamente el contrato BE-19.
- `GestionInventarioUI`: administración de categorías, unidades y estados.
- `OperacionPrestamosUI`: consulta de situación, validación previa y registro.
- `OperacionDevolucionesUI`: devoluciones, condiciones, incidencias e historial.
- `ClienteApi`: comunicación HTTP, serialización y errores comunes.
- `ProteccionRutas`: restricción de navegación según sesión y rol.

### Backend

- `AutenticacionAutorizacion`.
- `GestionPrestadores`.
- `GestionSolicitantes`.
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

`ActualizadorDisponibilidad` implementa BE-15 como Mediator mediante puertos
segregados para reservar, liberar, marcar incidencias y evaluar el cierre. Las
transiciones de unidad permanecen en `EstadoImplemento`; el Mediator no crea
transacciones propias.

## Consistencia y transacciones

El registro de préstamos valida usuario y unidades, guarda el préstamo y marca las unidades como PRESTADO dentro de una transacción. Bloqueos pesimistas serializan operaciones por solicitante y unidades, mientras un índice único parcial protege RN-06. Las consultas BE-11 separan la lectura del registro transaccional mediante `PrestamoConsultaServicio`, `DevolucionServicio`, `IncidenciaServicio` y `UsuarioConsultaServicio`, usan filtros paginados y proyecciones de conteos para evitar N+1; el vencimiento se calcula con el `Clock` sin mutar el préstamo. La situación del usuario reutiliza la cadena de elegibilidad en modo de solo lectura.

Registrar una devolución debe guardar la devolución, registrar una incidencia si aplica, actualizar el estado de la unidad, aplicar la política de sanción y evaluar el cierre del préstamo dentro del mismo límite transaccional cuando corresponda.

Las pruebas de concurrencia PostgreSQL/Testcontainers están definidas para el registro de préstamos; su ejecución requiere Docker.

## Principios de evolución

- Mantener trazabilidad `actor → contenedor → componente → código → prueba`.
- No introducir contenedores para sistemas externos excluidos del MVP.
- No convertir cada entidad en un componente arquitectónico.
- Preferir interfaces en los límites que requieran sustitución o pruebas.
- Mantener contratos explícitos entre frontend y backend.
- Registrar las decisiones que modifiquen el modelo C4 o los patrones aprobados.

