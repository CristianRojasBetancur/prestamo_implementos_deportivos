# Préstamo de Implementos Deportivos

Sistema web para consultar disponibilidad y gestionar préstamos de implementos deportivos.
El MVP está compuesto por una aplicación React/Vite, una API Java/Spring Boot y PostgreSQL.

## Requisitos

- Docker Desktop o Docker Engine con Compose v2.
- JDK 21 y Maven 3.9+ para ejecutar el backend fuera de Docker.
- Node.js 20+ y npm para ejecutar el frontend.

## Levantar todo con Docker

Desde la raíz del repositorio:

```bash
cp .env.example .env
# Edite .env y cambie DB_CONTRASENA y APP_JWT_SECRETO.
docker compose up -d --build
docker compose ps
```

La API queda disponible en `http://localhost:8080` y PostgreSQL en
`localhost:5432`. Flyway aplica las migraciones al arrancar la API. Si esos
puertos están ocupados, ajuste `API_PUERTO` o `DB_PUERTO` en `.env`.

```bash
curl http://localhost:8080/actuator/health
```

Para detener el stack:

```bash
docker compose down       # conserva los datos
docker compose down -v    # elimina también el volumen local
```

## Ejecutar frontend y backend en modo desarrollo

1. Configure la base y la API:

   ```bash
   cp apps/backend/.env.example apps/backend/.env
   # Edite apps/backend/.env y defina un APP_JWT_SECRETO propio.
   cd apps/backend
   docker compose up -d
   mvn spring-boot:run
   ```

2. En otra terminal, configure y levante la aplicación web:

   ```bash
   cd apps/frontend
   cp .env.example .env.local
   npm install
   npm run dev
   ```

La web queda en `http://localhost:5173` y usa
`VITE_API_BASE_URL=http://localhost:8080/api/v1`. El frontend nunca accede
directamente a PostgreSQL.

## Pruebas y empaquetado

```bash
cd apps/backend
mvn clean test
mvn clean package

cd ../frontend
npm test
npm run build
```

Las pruebas de integración del backend usan Testcontainers y requieren Docker.
La imagen del backend se construye desde la raíz con:

```bash
docker build --no-cache -t prestamo-backend ./apps/backend
```

## Smoke test

Con el Compose raíz levantado:

```bash
./scripts/smoke-test.sh
```

El script verifica health, readiness, disponibilidad pública y que una ruta
protegida rechace solicitudes sin autenticación. Para probar también un login,
defina `SMOKE_IDENTIFIER` y `SMOKE_PASSWORD` con credenciales de un entorno
local que tenga un usuario válido.

## API y documentación

- Health: `http://localhost:8080/actuator/health`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Especificación funcional: [`docs/specs/BE-01-api-backend-mvp.md`](docs/specs/BE-01-api-backend-mvp.md)
- Checklist de aceptación: [`docs/mvp-acceptance-checklist.md`](docs/mvp-acceptance-checklist.md)

## Alcance

El MVP cubre autenticación, autorización por rol, prestadores, inventario,
disponibilidad, préstamos, devoluciones, incidencias y sanciones. Permanecen
fuera de alcance ERP, mapas, geolocalización, notificaciones externas, pagos,
reservas independientes y analítica avanzada.
