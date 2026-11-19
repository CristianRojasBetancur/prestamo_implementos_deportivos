# Contexto de agentes — backend

Estas reglas aplican al trabajo dentro de `apps/backend/` y complementan el `AGENTS.md` de la raíz.

La aplicación está inicializada con Java 21, Spring Boot 4.1, Maven, Flyway y PostgreSQL. La especificación funcional base es `docs/specs/BE-01-api-backend-mvp.md`; BE-07 implementa categorías e inventario, BE-08 disponibilidad, BE-09 elegibilidad y BE-10 el registro transaccional de préstamos. Devoluciones e incidencias siguen pendientes. Mantener sincronizados aquí, en el `AGENTS.md` raíz y en `docs/agent-context/arquitectura-tecnica.md` los comandos, herramientas y convenciones que realmente existan.

## Comandos disponibles

Ejecutar desde `apps/backend/` (detalle en `README.md`):

- `docker compose up -d`: PostgreSQL 17 local con volumen `postgres-datos` y healthcheck. Requiere `.env` (copia de `.env.example`).
- `mvn spring-boot:run`: API en `http://localhost:8080` con el perfil `local`; aplica migraciones Flyway al arrancar.
- `mvn test`: pruebas con el perfil `test` y PostgreSQL efímero de Testcontainers (requiere Docker).
- `docker compose down` / `docker compose down -v`: detener la base (con `-v` se borran los datos).

## Estructura y configuración

- Paquete raíz `co.edu.itm.prestamosdeportivos`: `web` (controladores y DTO), `web.error` (manejo global de errores), `aplicacion` (servicios y casos de uso), `dominio` (entidades, estados y políticas), `persistencia` (repositorios), `configuracion` (cadena de seguridad, CORS, reloj, propiedades) y `seguridad` (JWT, contraseñas, principal autenticado).
- Perfiles: `local` (por defecto, lee `.env`), `test` (Testcontainers) y `prod` (todas las variables obligatorias, sin valores por defecto).
- Ningún secreto se versiona: credenciales y orígenes CORS provienen de variables de entorno. `.env` está en `.gitignore`.
- El esquema se gestiona solo con migraciones Flyway en `src/main/resources/db/migration` (`V<n>__descripcion.sql`); Hibernate usa `ddl-auto: validate`. No editar migraciones ya aplicadas (estrategia forward-only).
- El modelo de datos (tablas, constraints, índices, mapeo de enumeraciones y decisiones DM-xx) está en `docs/agent-context/modelo-datos.md`; toda migración que lo cambie debe actualizarlo.
- Las entidades JPA viven en `dominio` y los repositorios en `persistencia`. Los enums se persisten como `varchar` con el nombre de la constante y un `CHECK`; renombrar una constante exige migración.
- La gestión de categorías e inventario está en BE-07. Las unidades físicas no se borran; los cambios de estado requieren evento válido y se registran en `historial_estado_implemento` mediante Flyway V3.
- BE-08 expone disponibilidad pública agregada y paginada, sin códigos ni UUID de unidades; su prueba de carga RNF-01 está en `performance/availability.js` (k6, 100 VU, p95 < 1,5 s).
- La seguridad es stateless y deny-by-default; solo son públicas las rutas que BE-01 declara (salud, login, disponibilidad y categorías).
- Autenticación JWT (paquete `seguridad`): el rol se carga de la base de datos en cada solicitud; nunca se confía en un rol enviado por el cliente ni en el claim `rol`. El principal se obtiene con `@AuthenticationPrincipal UsuarioAutenticado`.
- Toda ruta nueva debe quedar cubierta por la matriz de `ConfiguracionSeguridad` y por `@PreAuthorize` en su controlador, y agregarse a `SeguridadAutenticacionTest.matriz()`.
- `mvn spring-boot:run` requiere `APP_JWT_SECRETO` en `.env`.
- La fecha y hora actuales se obtienen siempre del `Clock` inyectado, nunca de `LocalDateTime.now()` directo.
- Contratos y errores (obligatorio para todo endpoint): `docs/specs/BE-01-A-contratos-y-errores.md`.
  - Reutilizar los DTO de `web.dto` y los auxiliares de `web.comun` (`ParametrosPagina`, `PaginaRespuesta`, `ConversorFechas`); nunca devolver entidades ni `Page`.
  - Los errores de negocio se lanzan como `ExcepcionDominio(CodigoError, detalle)`; no se capturan excepciones de infraestructura en los servicios: `ManejadorGlobalErrores` las traduce.
  - No crear códigos de error ad hoc: agregarlos a `CodigoError` y al anexo BE-01-A.
  - Autorización por rol con `@PreAuthorize` en los controladores; los endpoints públicos se anotan con `@SecurityRequirements()` para OpenAPI.
- OpenAPI en `/v3/api-docs` y Swagger UI en `/swagger-ui.html` (local y pruebas; deshabilitados en producción salvo `APP_OPENAPI_HABILITADO=true`).

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

