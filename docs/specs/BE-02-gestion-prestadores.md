# BE-02 — Gestión de prestadores

## Objetivo

Implementar HU-02, HU-03 y HU-04 / RF-03…RF-06 para que `ADMIN` registre,
consulte, filtre, actualice y desactive prestadores, conservando su historial.

## Actores y autorización

- `ADMIN`: acceso a todas las operaciones de este módulo.
- `USER` y `PRESTADOR`: no pueden administrar prestadores.
- Prestadores inactivos no pueden autenticarse ni operar préstamos,
  devoluciones o incidencias. La autenticación valida `activo` y
  `versionCredenciales`, conforme a BE-01 §5. El estado inactivo persiste aunque
  no se haya implementado el módulo de autenticación u operaciones.

## Contratos

- `POST /api/v1/providers`: recibe `nombres`, `apellidos`, `identificacion`,
  `correo` y `contrasenaInicial`. Crea un `Usuario` con rol `PRESTADOR` y estado
  `ACTIVO`; responde `201 Created` y `Location`.
- `GET /api/v1/providers`: paginación mediante `pagina` (defecto 0), `tamano`
  (defecto 20, rango 1–100) y `orden`. Filtros parciales insensibles a
  mayúsculas/tildes: `nombres`, `identificacion`, `correo`; filtro exacto
  `estado=ACTIVO|INACTIVO`. También admite `q` para buscar por nombres,
  apellidos, identificación o correo. Orden por defecto: `nombres,asc`, con
  `id,asc` como desempate. Respuesta: `contenido`, `pagina`, `tamano`,
  `totalElementos`, `totalPaginas`.
- `GET /api/v1/providers/{id}`: consulta por UUID.
- `PATCH /api/v1/providers/{id}`: actualiza solo los campos presentes entre
  `nombres`, `apellidos`, `identificacion` y `correo`. No modifica rol, estado
  ni contraseña.
- `PATCH /api/v1/providers/{id}/estado`: recibe `{ "estado": "ACTIVO" }` o
  `{ "estado": "INACTIVO" }`. Al desactivar incrementa
  `versionCredenciales`, invalidando los tokens emitidos cuando la
  autenticación BE-01 esté integrada. No se agrega operación de borrado.

Las representaciones incluyen `id`, `nombres`, `apellidos`, `identificacion`,
`correo`, `rol`, `estado`, `creadoEn` y `actualizadoEn`; nunca incluyen
`contrasenaHash`. El rol devuelto siempre es `PRESTADOR`. Los timestamps se
serializan con la zona institucional configurada.

La contraseña inicial se valida con mínimo 8 caracteres y se persiste como
BCrypt con factor de trabajo mínimo 10, según BE-01 §5. No se generan ni envían
notificaciones.

Para evitar duplicados con espacios accidentales, se recortan los extremos de
identificación, nombres y apellidos. El correo se recorta y normaliza a
minúsculas; su unicidad se compara sin distinguir mayúsculas.

## Errores

- Solicitud sintáctica o campos inválidos: `400 VALIDACION_FALLIDA`, con errores
  por campo.
- Identificación o correo duplicados: `409 CONFLICTO_UNICIDAD`, con el campo
  que produjo el conflicto.
- UUID inválido: `400 SOLICITUD_INVALIDA`; prestador inexistente o de otro rol:
  `404 RECURSO_NO_ENCONTRADO`.
- Sin autenticación: `401`; autenticado con rol distinto de `ADMIN`: `403`.
- Formato de errores: Problem Details de BE-01 §4.8.

## Decisión de compatibilidad

BE-01 define el mismo comportamiento, pero registra las rutas `/prestadores` y
`PUT` para actualización completa. Esta issue requiere explícitamente
`/providers` y `PATCH`; por ello esta especificación sustituye esas rutas solo
para este módulo. Conserva de BE-01 los campos en español, roles, credencial
`contrasenaInicial`, BCrypt, UUID, paginación y formato de errores. No se crean
alias adicionales.

## Criterios de aceptación

1. `ADMIN` registra un prestador válido y recibe 201 y `Location`.
2. Campos obligatorios faltantes/formato inválido y duplicados no crean el
   registro e informan campo y problema.
3. `ADMIN` consulta, filtra y pagina resultados deterministas.
4. `ADMIN` actualiza datos permitidos; campos desconocidos o protegidos no se
   modifican.
5. Desactivar conserva el registro consultable, marca `INACTIVO` e incrementa
   `versionCredenciales`; al integrarse BE-01, bloquea autenticación y
   operaciones.
6. `USER` y `PRESTADOR` reciben 403 al intentar administrar prestadores; sin
   token reciben 401.
7. La persistencia no ofrece borrado físico de prestadores.
8. Hay pruebas de controller, servicio, repositorio, autorización e integración
   con matriz de roles, duplicidad, paginación e historial.
9. HU-02…HU-04 / RF-03…RF-06 tienen trazabilidad a los casos de uso CU-03…CU-06
   de BE-01.

## Dependencias

Se apoya en la base backend y BE-01, integradas desde
`origin/feat/backend-inicializacion`. BE-03…BE-05 no están presentes en el
remoto actual y no son necesarios para estos endpoints. Como aún no existe el
módulo JWT ni los casos de uso de préstamos, devoluciones e incidencias, esta
implementación almacena el estado y versión de credenciales correctamente; la
validación efectiva de tokens y operaciones corresponde a la integración de
esos módulos según BE-01 §5.

## Trazabilidad

- HU-02 / CU-03 / RF-03: registro.
- HU-03 / CU-04 / RF-04, RF-05: consulta, búsqueda y filtros.
- HU-04 / CU-05, CU-06 / RF-06: actualización y cambio de estado.
- Fuente: `docs/specs/BE-01-api-backend-mvp.md` y
  `docs/source/entregable_3.txt`.
