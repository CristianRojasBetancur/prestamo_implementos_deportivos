# API backend — Préstamo de Implementos Deportivos

API REST del MVP, gobernada por la especificación
[`docs/specs/BE-01-api-backend-mvp.md`](../../docs/specs/BE-01-api-backend-mvp.md).

| Tecnología | Versión |
|---|---|
| Java | 21 (LTS) |
| Spring Boot | 4.1.2 (Spring MVC, Validation, Data JPA, Security, Actuator) |
| Documentación API | springdoc-openapi 3.1.1 (OpenAPI 3.1, Swagger UI) |
| Migraciones | Flyway |
| Base de datos | PostgreSQL 17 (Docker Compose en local, Testcontainers en pruebas) |
| Build | Maven 3.9+ |

> Estado: base técnica y esquema de persistencia del MVP (V2) inicializados.
> Todavía no hay casos de uso de negocio, endpoints de dominio ni datos de
> demostración. Modelo de datos: [`docs/agent-context/modelo-datos.md`](../../docs/agent-context/modelo-datos.md).

## Requisitos

- JDK 21 (`java -version`).
- Maven 3.9 o superior (`mvn -v`).
- Docker Desktop o Docker Engine con Compose v2 (`docker compose version`), en
  ejecución. Lo usan tanto la base local como las pruebas (Testcontainers).

## Puesta en marcha local

Todos los comandos se ejecutan desde `apps/backend/`.

1. Crear el archivo de variables locales (no se versiona):

   ```bash
   cp .env.example .env            # Linux/macOS
   Copy-Item .env.example .env     # Windows PowerShell
   ```

   Cambie `DB_CONTRASENA` por un valor propio. Docker Compose y el perfil
   `local` de Spring leen el mismo archivo.

2. Levantar PostgreSQL y esperar a que esté `healthy`:

   ```bash
   docker compose up -d
   docker compose ps
   ```

3. Ejecutar la API (perfil `local` por defecto, puerto 8080):

   ```bash
   mvn spring-boot:run
   ```

   Al arrancar, Flyway aplica las migraciones pendientes de
   `src/main/resources/db/migration`.

4. Comprobar la salud de la aplicación y de la base de datos:

   ```bash
   curl http://localhost:8080/actuator/health
   # {"status":"UP","groups":["liveness","readiness"]}
   ```

   En PowerShell: `Invoke-RestMethod http://localhost:8080/actuator/health`.
   Si PostgreSQL no está disponible, el endpoint responde `503` con
   `"status":"DOWN"`.

## Pruebas

```bash
mvn test
```

Las pruebas usan el perfil `test` y levantan un PostgreSQL efímero con
Testcontainers (requiere Docker en ejecución, no requiere el `compose.yaml`).
Cubren el arranque del contexto, la aplicación de migraciones Flyway, el
healthcheck, la política deny-by-default (`401`), CORS, los constraints e
índices del esquema y las consultas de los repositorios.

## Detener

```bash
# Detener la API: Ctrl + C en la terminal de mvn spring-boot:run
docker compose stop        # detiene PostgreSQL y conserva los datos
docker compose down        # elimina el contenedor y conserva el volumen
docker compose down -v     # elimina también el volumen (base vacía la próxima vez)
```

## Perfiles y variables de entorno

| Perfil | Uso | Base de datos |
|---|---|---|
| `local` (por defecto) | Desarrollo con `mvn spring-boot:run` | PostgreSQL de `compose.yaml`, credenciales de `.env` |
| `test` | `mvn test` | Testcontainers (`postgres:17-alpine`) |
| `prod` | Despliegue (`SPRING_PROFILES_ACTIVE=prod`) | `DB_URL`, sin valores por defecto |

| Variable | Perfil | Obligatoria | Descripción |
|---|---|:-:|---|
| `DB_NOMBRE`, `DB_USUARIO`, `DB_CONTRASENA` | local | Sí | Base, usuario y contraseña de PostgreSQL local. |
| `DB_HOST`, `DB_PUERTO` | local | No | Por defecto `localhost` y `5432`. |
| `DB_URL` | prod | Sí | URL JDBC, por ejemplo `jdbc:postgresql://host:5432/prestamos`. |
| `DB_USUARIO`, `DB_CONTRASENA` | prod | Sí | Credenciales de la base. |
| `APP_CORS_ORIGENES` | local, prod | Sí en prod | Orígenes exactos del frontend separados por coma. No admite `*`. Por defecto en local: `http://localhost:5173`. |
| `APP_ZONA_HORARIA` | todos | No | Zona institucional. Por defecto `America/Bogota`. |
| `APP_OPENAPI_HABILITADO` | prod | No | `true` para publicar OpenAPI/Swagger en producción. Por defecto `false`. |
| `SERVER_PORT` | todos | No | Por defecto `8080`. |
| `SPRING_PROFILES_ACTIVE` | — | No | `local` si no se define. |

El archivo `.env` se busca en el directorio de trabajo: ejecute la aplicación
desde `apps/backend/`, o defina las variables en el sistema o en la
configuración de ejecución del IDE. Ningún secreto real se versiona.

Para que el frontend consuma esta API en local, defina en `apps/frontend`
`VITE_API_BASE_URL=http://localhost:8080/api/v1`.

## Estructura de paquetes

Paquete raíz: `co.edu.itm.prestamosdeportivos`

| Paquete | Contiene |
|---|---|
| `web` | Controladores REST (`*Controlador`). |
| `web.dto` | DTO de solicitud y respuesta de cada caso de uso de BE-01. |
| `web.comun` | Paginación (`ParametrosPagina`, `PaginaRespuesta`) y conversión de fechas. |
| `web.error` | Manejador global de errores (`ProblemaRespuesta`, `application/problem+json`). |
| `dominio.error` | Catálogo `CodigoError` y `ExcepcionDominio` (sin dependencias HTTP). |
| `aplicacion` | Servicios de aplicación (`*Servicio`), interfaces de casos de uso y transacciones. |
| `dominio` | Entidades JPA, enumeraciones de estado, invariantes y políticas (State, Chain, Strategy). |
| `persistencia` | Repositorios Spring Data (`*Repositorio`) con las consultas mínimas. |
| `configuracion` | Seguridad, CORS, reloj (`Clock`) y propiedades tipadas. |

## Endpoints disponibles

| Ruta | Acceso | Descripción |
|---|---|---|
| `GET /actuator/health` | Público | Estado de la aplicación y de la base de datos. |
| `GET /actuator/health/liveness`, `/readiness` | Público | Sondas para el despliegue. |
| `GET /v3/api-docs` | Público (local/pruebas) | Documento OpenAPI 3.1. |
| `GET /swagger-ui.html` | Público (local/pruebas) | Swagger UI para explorar la API. |

Cualquier otra ruta responde `401` mientras no se implemente la autenticación
(BE-01 §5). Todas las respuestas de error usan `application/problem+json` con un
`codigo` estable e incluyen la cabecera `X-Correlation-Id`; el formato está en
[`docs/specs/BE-01-A-contratos-y-errores.md`](../../docs/specs/BE-01-A-contratos-y-errores.md).

## Migraciones

- Ubicación: `src/main/resources/db/migration`.
- Nombre: `V<n>__descripcion_en_snake_case.sql` (por ejemplo
  `V2__crear_tabla_usuario.sql`).
- Una migración aplicada nunca se edita; cualquier cambio se hace con una
  migración nueva. Hibernate solo valida el esquema (`ddl-auto: validate`).
- Aplicadas: `V1__linea_base.sql` y `V2__crear_esquema_mvp.sql` (9 tablas del MVP).
- Reversión: forward-only. En local, `docker compose down -v` recrea la base
  vacía; en entornos compartidos se usa una migración correctiva. Detalle en
  `docs/agent-context/modelo-datos.md` §6.
