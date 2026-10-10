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

