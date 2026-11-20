# FE-02 — Integración de la aplicación web con la API

| Campo | Valor |
|---|---|
| Contenedor | Aplicación web |
| Estado | Implementada |
| Dependencias | BE-01, BE-02, BE-07, BE-08, BE-09, BE-10, BE-11, BE-12, BE-14, BE-15 y BE-19 |

## Objetivo

Hacer que la aplicación React/Vite consuma los contratos cerrados de la API sin
duplicar reglas de negocio, simular operaciones ni mantener información de
demostración como fuente de las pantallas.

## Alcance

- Cliente HTTP único, tipado y configurable mediante `VITE_API_BASE_URL`.
- Sesión JWT en `sessionStorage`, restauración mediante `/auth/me` y cierre con
  `/auth/logout`.
- Adaptadores para autenticación, disponibilidad, categorías, prestadores,
  inventario, usuarios/situación, préstamos, devoluciones e incidencias.
- Estados de carga, vacío, API no configurada, error y éxito para los flujos
  visibles.
- Formularios y tablas alineados con los DTO del backend, sus enums y fechas
  ISO-8601 con zona horaria.
- Protección de navegación por rol como ayuda de interfaz; la autorización
  real permanece en el backend.

## Contrato frontend

El cliente envía `Authorization: Bearer <token>` en operaciones autenticadas y
`X-Correlation-Id` en cada solicitud. Los errores `application/problem+json`
se convierten en `ApiError` con `status`, `code`, `message`, errores por campo y
correlación. Las vistas no muestran trazas, nombres de constraints ni mensajes
internos.

| Flujo | Rutas |
|---|---|
| Autenticación | `/auth/login`, `/auth/me`, `/auth/logout` |
| Catálogo | `/availability`, `/categorias` |
| Prestadores | `/providers` |
| Inventario | `/inventory/units` |
| Préstamos | `/usuarios`, `POST /usuarios`, `/usuarios/{id}/situacion`, `/implementos/prestables`, `/prestamos` |
| Devoluciones | `/devoluciones`, `/prestamos/{id}/devoluciones` |
| Incidencias | `/incidencias`, `/prestamos/{id}/incidencias`, `/incidencias/{id}/resolucion` |

No se agregan endpoints para completar una pantalla. Si un contrato no está
disponible, la interfaz muestra el estado correspondiente y no confirma la
operación.

Para registrar devoluciones, el formulario selecciona inicialmente todas las
unidades con estado `PENDIENTE_DEVOLUCION`. Cada unidad puede desmarcarse para
registrar una devolución parcial; las unidades desmarcadas permanecen
pendientes y se excluyen de `POST /prestamos/{id}/devoluciones`. La interfaz
deshabilita visualmente sus controles, conserva sus valores al volver a
marcarlas y no permite enviar una solicitud sin unidades seleccionadas. El
backend conserva la decisión autoritativa del alcance de la devolución y
calcula `PARCIAL` o `COMPLETA` a partir de los elementos recibidos.

El botón `Registrar devolución` de la pantalla de devoluciones primero muestra
un listado de préstamos en estados `VIGENTE`, `DEVOLUCION_PARCIAL` o `VENCIDO`.
El usuario debe seleccionar explícitamente un préstamo; solo después se
consulta su detalle y se abre el formulario de devolución. No se selecciona
automáticamente ningún préstamo.

## Reglas de responsabilidad

- El backend decide elegibilidad, límites, disponibilidad, estados, sanciones,
  autorización y fechas de préstamo/devolución.
- El alta de un solicitante se realiza mediante `POST /usuarios` con los tres
  datos operativos definidos por BE-19; al responder `201`, el frontend usa el
  DTO devuelto para continuar el asistente y vuelve a consultar su situación.
- El frontend solo valida presencia y formato básico necesario para construir
  un formulario accesible; nunca sustituye la respuesta de la API.
- Los resultados remotos pueden mantenerse temporalmente en estado React para
  renderizar la vista, pero no existen fixtures ni resultados simulados.
- El catálogo público usa el agregado de disponibilidad y no muestra UUID ni
  códigos de unidades físicas.

## Criterios de aceptación

1. El login usa `identificador` y `contrasena`, conserva el JWT en
   `sessionStorage` y construye la navegación con el rol recibido por la API.
2. El catálogo representa datos, vacío, error y API no configurada sin inventar
   disponibilidad.
3. Prestadores, inventario, préstamos, devoluciones e incidencias usan los DTO
   reales y no datos de demostración.
4. 401, 403, 409, 422 y errores de red se muestran de forma comprensible y no
   se tratan como éxito.
5. Los estados visibles y fechas se traducen únicamente para presentación; los
   payloads conservan los enums y nombres del backend.
6. No hay conexión del frontend a PostgreSQL ni reglas de negocio autoritativas
   duplicadas.

## Verificación

- `npm test` ejecuta pruebas del cliente HTTP, errores normalizados, sesión,
  catálogo y asistente de préstamo.
- `npm run build` valida tipos y compilación de producción.
- Smoke test manual con PostgreSQL, backend y frontend levantados según las
  guías de ambos contenedores.
