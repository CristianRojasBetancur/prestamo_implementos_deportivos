# Contexto de agentes — backend

Estas reglas aplican al trabajo dentro de `apps/backend/` y complementan el `AGENTS.md` de la raíz.

La aplicación está inicializada con Java 21, Spring Boot 4.1, Maven, Flyway y PostgreSQL. La especificación funcional es `docs/specs/BE-01-api-backend-mvp.md`. Mantener sincronizados aquí, en el `AGENTS.md` raíz y en `docs/agent-context/arquitectura-tecnica.md` los comandos, herramientas y convenciones que realmente existan.

## Comandos disponibles

Ejecutar desde `apps/backend/` (detalle en `README.md`):

- `docker compose up -d`: PostgreSQL 17 local con volumen `postgres-datos` y healthcheck. Requiere `.env` (copia de `.env.example`).
- `mvn spring-boot:run`: API en `http://localhost:8080` con el perfil `local`; aplica migraciones Flyway al arrancar.
- `mvn test`: pruebas con el perfil `test` y PostgreSQL efímero de Testcontainers (requiere Docker).
- `docker compose down` / `docker compose down -v`: detener la base (con `-v` se borran los datos).

## Estructura y configuración

- Paquete raíz `co.edu.itm.prestamosdeportivos`: `web` (controladores y DTO), `web.error` (manejo global de errores), `aplicacion` (servicios y casos de uso), `dominio` (entidades, estados y políticas), `persistencia` (repositorios) y `configuracion` (seguridad, CORS, reloj, propiedades).
- Perfiles: `local` (por defecto, lee `.env`), `test` (Testcontainers) y `prod` (todas las variables obligatorias, sin valores por defecto).
- Ningún secreto se versiona: credenciales y orígenes CORS provienen de variables de entorno. `.env` está en `.gitignore`.
- El esquema se gestiona solo con migraciones Flyway en `src/main/resources/db/migration` (`V<n>__descripcion.sql`); Hibernate usa `ddl-auto: validate`. No editar migraciones ya aplicadas.
- La seguridad es stateless y deny-by-default; solo son públicas las rutas que BE-01 declara (salud, login, disponibilidad y categorías).
- La fecha y hora actuales se obtienen siempre del `Clock` inyectado, nunca de `LocalDateTime.now()` directo.

## Arquitectura

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

