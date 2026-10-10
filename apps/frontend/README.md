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
base de la API cuando el backend y sus contratos estén disponibles. Sin esa
configuración, la interfaz conserva sus estados vacíos y no simula operaciones.

## Comandos

```sh
npm test
npm run build
npm run preview
```
