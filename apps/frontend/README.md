# Frontend — Cancha

Aplicación React/Vite/TypeScript para el Sistema de Préstamo de Implementos
Deportivos. El alcance inicial está definido en
`../../docs/specs/FE-01-frontend-mvp.md`.

## Desarrollo local

```sh
npm install
npm run dev
```

Configura `VITE_API_BASE_URL` en un archivo `.env.local` para indicar la URL
base de la API:

```sh
cp .env.example .env.local
```

El valor local esperado es `http://localhost:8080/api/v1`. Sin esa
configuración, la interfaz muestra el estado de servicio no configurado y no
simula datos ni operaciones.

## Comandos

```sh
npm test
npm run build
npm run preview
```

## Ejecución completa local

Desde `apps/backend/`, levanta PostgreSQL y la API:

```sh
cp .env.example .env
docker compose up -d
mvn spring-boot:run
```

En otra terminal, desde `apps/frontend/`:

```sh
cp .env.example .env.local
npm install
npm run dev
```

La API permite por defecto el origen `http://localhost:5173` mediante
`APP_CORS_ORIGENES`. No se versionan credenciales ni tokens.
