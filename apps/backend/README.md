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

> Estado: backend funcional con autenticación JWT, gestión de prestadores,
> inventario, elegibilidad, préstamos, devoluciones, incidencias y sanciones.

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
mvn clean test
```

Las pruebas usan el perfil `test` y levantan un PostgreSQL efímero con
Testcontainers (requiere Docker en ejecución, no requiere el `compose.yaml`).
`mvn test` ejecuta la misma suite unitaria y de integración; `mvn clean test`
se recomienda para verificar un entorno recién clonado.
No leen ni escriben la base local: cada suite crea su contenedor, Flyway aplica
las migraciones y FlujoCompletoApiIntegracionTest crea un escenario aislado
con credenciales exclusivas de prueba. La suite cubre el flujo HTTP completo y
errores de autenticación, autorización, duplicados, disponibilidad, elegibilidad,
unidad ajena, operación repetida, devolución parcial/final, incidencia,
sanción y cierre.

Para ejecutar solo el flujo E2E:

    mvn -Dtest=FlujoCompletoApiIntegracionTest test

La matriz de escenarios, HU/RN/RNF cubiertas y la revisión de flakiness están
documentadas en TEST-TRACEABILITY.md.

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
| `test` | `mvn clean test` | Testcontainers (`postgres:17-alpine`), sin datos ficticios en local/prod |
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
| `APP_JWT_SECRETO` | local, prod | Sí | Secreto HS256 de 32 bytes o más. Ver `.env.example` para generarlo. |
| `APP_JWT_EXPIRACION` | todos | No | Duración del token (ISO-8601). Por defecto `PT60M`. |
| `APP_ADMIN_CORREO`, `APP_ADMIN_CONTRASENA`, `APP_ADMIN_IDENTIFICACION` | todos | No | Crea el primer ADMIN al arrancar si no existe (contraseña de 12+ caracteres). |
| `DB_POOL_MAX`, `DB_POOL_MIN` | todos | No | Pool Hikari; por defecto 20 conexiones máximas y 5 inactivas mínimas. |
| `DB_POOL_CONNECTION_TIMEOUT_MS`, `DB_POOL_VALIDATION_TIMEOUT_MS` | todos | No | Timeouts de adquisición y validación de conexión; por defecto 2000 ms y 1000 ms. |
| `DB_QUERY_TIMEOUT_SECONDS`, `APP_TRANSACTION_TIMEOUT_SECONDS` | todos | No | Límite JDBC de consulta y transacción; por defecto 2 s y 5 s. |
| `SERVER_MAX_REQUEST_HEADER_SIZE` | todos | No | Tamaño máximo de headers HTTP; por defecto 16 KiB. |
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
| `configuracion` | Cadena de seguridad, CORS, correlación, OpenAPI, reloj (`Clock`) y propiedades tipadas. |
| `seguridad` | JWT (emisión y validación), hash de contraseñas, principal `UsuarioAutenticado` y revocación. |

## Endpoints disponibles

| Ruta | Acceso | Descripción |
|---|---|---|
| `GET /actuator/health` | Público | Estado de la aplicación y de la base de datos. |
| `GET /actuator/health/liveness`, `/readiness` | Público | Sondas para el despliegue. |
| `POST /api/v1/auth/login` | Público | Inicia sesión y emite un JWT para ADMIN o PRESTADOR. |
| `GET /api/v1/auth/me` | Autenticado | Consulta la sesión actual. |
| `POST /api/v1/auth/logout` | Autenticado | Revoca las sesiones del usuario. |
| `GET /api/v1/availability` / `/disponibilidad` | Público | Consulta agregada de disponibilidad. |
| `GET /api/v1/categorias` | Público | Lista categorías activas. |
| `POST/GET/PUT /api/v1/categorias/{id}` | `ADMIN` | Gestiona categorías. |
| `POST/GET/PATCH /api/v1/inventory/units` | `ADMIN` | Gestiona unidades del inventario. |
| `POST /api/v1/providers` | `ADMIN` | Registra un prestador con contraseña inicial. |
| `GET /api/v1/providers` | `ADMIN` | Lista prestadores con filtros y paginación. |
| `GET /api/v1/providers/{id}` | `ADMIN` | Consulta un prestador, incluso inactivo. |
| `PATCH /api/v1/providers/{id}` | `ADMIN` | Actualiza nombres, apellidos, identificación o correo. |
| `PATCH /api/v1/providers/{id}/estado` | `ADMIN` | Activa o desactiva conservando el historial. |
| `GET /api/v1/prestamos` / `/api/v1/loans` | `PRESTADOR` | Lista préstamos con paginación y filtros. |
| `GET /api/v1/prestamos/{id}` / `/api/v1/loans/{id}` | `PRESTADOR` | Consulta el detalle, devoluciones e incidencias. |
| `POST /api/v1/prestamos` / `/api/v1/loans` | `PRESTADOR` | Registra un préstamo de 1 a 4 unidades. |
| `GET /api/v1/usuarios/{userId}/situacion` / `/users/{userId}/loan-situation` | `PRESTADOR` | Consulta la situación de préstamos del usuario. |

Las rutas de prestadores requieren rol `ADMIN`; reciben `401` sin autenticación
y `403` con otro rol. La autenticación JWT se obtiene mediante
`POST /api/v1/auth/login`.

## Colección Postman

La colección importable está en
[`docs/postman/prestamo-implementos-deportivos.postman_collection.json`](../../docs/postman/prestamo-implementos-deportivos.postman_collection.json).
Después de importarla, configure `admin_identifier`, `admin_password`,
`provider_identifier` y `provider_password`; ejecute el login correspondiente
antes de llamar las rutas protegidas. El token se guarda automáticamente en
`access_token`.

## Migraciones

- Ubicación: `src/main/resources/db/migration`.
- Nombre: `V<n>__descripcion_en_snake_case.sql` (por ejemplo
  `V2__crear_tabla_usuario.sql`).
- Una migración aplicada nunca se edita; cualquier cambio se hace con una
  migración nueva. Hibernate solo valida el esquema (`ddl-auto: validate`).
- Aplicadas: V1__linea_base.sql a V10__indices_listados_operativos.sql.
- Reversión: forward-only. En local, `docker compose down -v` recrea la base
  vacía; en entornos compartidos se usa una migración correctiva. Detalle en
  `docs/agent-context/modelo-datos.md` §6.

## Medición RNF-01 de disponibilidad

Con la API local levantada, ejecute el escenario k6 de 100 usuarios virtuales durante un minuto:

```bash
k6 run -e BASE_URL=http://localhost:8080 performance/availability.js
```

El umbral configurado es p95 < 1,5 s y menos de 1 % de respuestas fallidas. Ejecute la medición con datos de volumen representativo y revise el plan con `EXPLAIN (ANALYZE, BUFFERS)` sobre la consulta agregada de `DisponibilidadRepositorio`.

Para medir las cinco familias críticas bajo 100 usuarios virtuales, consulte
[`performance/README.md`](performance/README.md) y ejecute `critical-api.js`
con tokens temporales de `ADMIN` y `PRESTADOR`. Los resultados, el dataset, la versión de
la JVM, la configuración del pool y los planes de PostgreSQL deben conservarse
como evidencia de BE-18. El endpoint `/actuator/health/readiness` incorpora la
disponibilidad de PostgreSQL; `/actuator/health/liveness` solo representa el
estado de la aplicación. La disponibilidad mensual del 99,5 % no se garantiza
desde el entorno local.
