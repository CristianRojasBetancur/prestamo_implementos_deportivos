# BE-01-A — Contratos comunes y formato de errores de la API

| Campo | Valor |
|---|---|
| Identificador | `BE-01-A` (anexo de `BE-01-api-backend-mvp.md`) |
| Estado | Implementado (capa transversal del backend) |
| Alcance | Formato de errores, códigos, validación, serialización, paginación, correlación, logs y OpenAPI |
| Código | `apps/backend`: paquetes `dominio.error`, `web.error`, `web.comun`, `web.dto`, `configuracion` |

Este anexo detalla y amplía BE-01 §4.4 a §4.9. Todo endpoint del backend debe
cumplirlo. Si hay diferencias, este anexo prevalece sobre los ejemplos de BE-01
en lo que respecta al formato de error.

## 1. Formato de error

Toda respuesta de error usa `Content-Type: application/problem+json` y el cuerpo
`ProblemaRespuesta` (RFC 9457 extendido):

```json
{
  "type": "https://prestamos.itm.edu.co/errores/validacion-fallida",
  "title": "Datos inválidos",
  "status": 400,
  "detail": "Uno o más campos no son válidos.",
  "instance": "/api/v1/prestamos",
  "codigo": "VALIDACION_FALLIDA",
  "timestamp": "2026-08-31T18:00:00-05:00",
  "correlacionId": "3f1c2a4e-9b8d-4c2a-9e51-0a7d3f2b1c11",
  "errores": [
    { "campo": "implementoIds", "mensaje": "debe contener al menos un elemento" },
    { "campo": "items[1].incidencia.descripcion", "mensaje": "es obligatorio" }
  ]
}
```

| Campo | Obligatorio | Equivalente en el issue | Descripción |
|---|:-:|---|---|
| `type` | Sí | — | URI estable del tipo: `https://prestamos.itm.edu.co/errores/<codigo-en-kebab-case>`. |
| `title` | Sí | — | Título corto del código, en español. |
| `status` | Sí | status | Igual al estado HTTP. |
| `detail` | Sí | mensaje | Mensaje en español para la persona usuaria. Nunca contiene SQL, trazas, nombres de clases ni de constraints. |
| `instance` | Sí | path | Ruta de la solicitud (sin parámetros de consulta). |
| `codigo` | Sí | código | Código estable del catálogo (§2). **El frontend decide con este campo.** |
| `timestamp` | Sí | timestamp | ISO-8601 con desplazamiento de la zona institucional. |
| `correlacionId` | Sí* | — | Igual a la cabecera `X-Correlation-Id` (§6). *Se omite solo si el error ocurre fuera del filtro. |
| `errores` | No | errores por campo | Lista de `{campo, mensaje}`. Se omite cuando no aplica. `campo` usa la notación del JSON recibido. |

## 2. Catálogo de códigos

Los códigos son constantes de `CodigoError`. Un código **nunca se renombra ni se
reutiliza** con otro significado.

| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `SOLICITUD_INVALIDA` | JSON mal formado, tipos o enumeraciones inválidos en el cuerpo, fecha sin zona horaria. |
| 400 | `VALIDACION_FALLIDA` | Bean Validation, parámetros de ruta o consulta con formato inválido, parámetros obligatorios ausentes, paginación u orden fuera de lo permitido. Incluye `errores`. |
| 401 | `NO_AUTENTICADO` | Sin token o con token inválido. |
| 401 | `CREDENCIALES_INVALIDAS` | Login fallido (BE-01 §5). |
| 401 | `SESION_INVALIDA` | Token expirado o revocado. |
| 403 | `ACCESO_DENEGADO` | Autenticado, pero el rol no permite la operación. |
| 404 | `RECURSO_NO_ENCONTRADO` | Recurso de la ruta o referenciado inexistente; ruta inexistente. |
| 405 | `METODO_NO_PERMITIDO` | Método HTTP no soportado por la ruta. |
| 406 | `TIPO_CONTENIDO_NO_ACEPTABLE` | `Accept` incompatible con JSON. |
| 409 | `CONFLICTO_UNICIDAD` | Duplicado (código, correo, identificación, nombre). |
| 409 | `CONFLICTO_INTEGRIDAD` | Otra violación de integridad de la base de datos. |
| 409 | `CONFLICTO_CONCURRENCIA` | Bloqueo optimista o pesimista; otra operación modificó los datos. |
| 409 | `IMPLEMENTO_NO_DISPONIBLE`, `TRANSICION_INVALIDA`, `PRESTAMO_FINALIZADO`, `UNIDAD_YA_RESUELTA` | Conflictos de estado de BE-01. |
| 415 | `TIPO_CONTENIDO_NO_SOPORTADO` | El cuerpo no es `application/json`. |
| 422 | `LIMITE_IMPLEMENTOS_EXCEDIDO`, `USUARIO_NO_HABILITADO`, `USUARIO_SANCIONADO`, `USUARIO_CON_PRESTAMOS_VENCIDOS`, `INCIDENCIA_REQUERIDA`, `IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO`, `TIPO_INCIDENCIA_NO_PERMITIDO`, `JUSTIFICACION_REQUERIDA`, `CATEGORIA_INACTIVA` | Reglas de negocio de BE-01. |
| 500 | `ERROR_INTERNO` | Error no previsto. Mensaje genérico; la traza solo va al log, con el `correlacionId`. |
| 503 | `SERVICIO_NO_DISPONIBLE` | La base de datos u otra dependencia no responde. |

Códigos nuevos respecto al catálogo original de BE-01: `CONFLICTO_INTEGRIDAD`,
`METODO_NO_PERMITIDO`, `TIPO_CONTENIDO_NO_ACEPTABLE`,
`TIPO_CONTENIDO_NO_SOPORTADO` y `SERVICIO_NO_DISPONIBLE`. La regla para
distinguir 409 de 422 es la de BE-01 §4.9.

## 3. Cómo producir errores en el código

- **Servicios y dominio:** lanzan `ExcepcionDominio(CodigoError, detalle[, errores])`.
  El `detalle` se muestra tal cual, así que debe redactarse para la persona
  usuaria. El estado HTTP se deriva del `TipoError` del código; el dominio no
  conoce HTTP.
- **Validación de forma:** anotaciones Bean Validation en los DTO con los
  mensajes de `MensajesValidacion`.
- **Infraestructura:** no se captura nada en los servicios. El manejador traduce:

| Excepción | Respuesta |
|---|---|
| `ExcepcionDominio` | Estado según su `TipoError`, con el código y detalle propios. |
| `MethodArgumentNotValidException`, `HandlerMethodValidationException` | 400 `VALIDACION_FALLIDA` con `errores`. |
| `TypeMismatchException` (UUID, enum o fecha inválidos en ruta o consulta) | 400 `VALIDACION_FALLIDA` con el parámetro. |
| `MissingServletRequestParameterException` | 400 `VALIDACION_FALLIDA`. |
| `HttpMessageNotReadableException` | 400 `SOLICITUD_INVALIDA`. |
| `AuthenticationException` (filtro o controlador) | 401 (`CREDENCIALES_INVALIDAS` si es `BadCredentialsException`). |
| `AccessDeniedException` | 403, o 401 si la petición es anónima. |
| `DataIntegrityViolationException` | 409 `CONFLICTO_UNICIDAD` (SQLState 23505) o `CONFLICTO_INTEGRIDAD`. |
| `OptimisticLockingFailureException`, `PessimisticLockingFailureException` | 409 `CONFLICTO_CONCURRENCIA`. |
| `DataAccessResourceFailureException`, `CannotCreateTransactionException` | 503 `SERVICIO_NO_DISPONIBLE`. |
| Excepciones estándar de Spring MVC (404 de ruta, 405, 406, 415…) | Código del catálogo según el estado. |
| Cualquier otra | 500 `ERROR_INTERNO`. |

Los 401 y 403 que se producen en la cadena de Spring Security se delegan al
mismo manejador (`DelegacionErroresSeguridad`), así que tienen idéntico formato.

## 4. DTO y validación

- Cada caso de uso de BE-01 §10 tiene sus records de solicitud y respuesta en
  `web.dto` (`AutenticacionDto`, `CatalogoDto`, `PrestadorDto`, `InventarioDto`,
  `UsuarioDto`, `PrestamoDto`, `DevolucionDto`, `IncidenciaDto`, `Referencias`).
  Los endpoints nuevos **reutilizan** estos DTO; si un contrato cambia, se
  actualiza el DTO y BE-01.
- Los controladores nunca devuelven entidades JPA ni `Page` de Spring.
- Ningún DTO de respuesta incluye contraseñas, hashes, `versionCredenciales`,
  `version` ni otros campos internos. Los DTO que reciben contraseñas o tokens
  ocultan esos valores en `toString()` para que no aparezcan en logs.
- Los mensajes de validación están en español (`MensajesValidacion`).

## 5. Serialización y paginación

- **Fechas:** los DTO de respuesta usan `OffsetDateTime` y Jackson las serializa
  como `2026-08-31T18:00:00-05:00`. `ConversorFechas` convierte los `Instant`
  persistidos a la zona `app.zona-horaria`. En la entrada, una fecha sin
  desplazamiento se rechaza con 400.
- **Enumeraciones:** se serializan y reciben por nombre (`VIGENTE`,
  `EN_MANTENIMIENTO`); un valor desconocido produce 400.
- **Identificadores:** UUID; uno mal formado produce 400 `VALIDACION_FALLIDA`.
- **Paginación:** `ParametrosPagina` (`pagina` ≥ 0, `tamano` 1–100, `orden` con el
  formato `campo,asc|desc`) y `PaginaRespuesta<T>` (`contenido`, `pagina`,
  `tamano`, `totalElementos`, `totalPaginas`). Cada endpoint declara sus campos
  de orden permitidos; uno no permitido produce 400 con `errores[campo=orden]`.
  En los controladores se usa
  `@Valid @ParameterObject ParametrosPagina pagina` para que OpenAPI documente
  los parámetros.

## 6. Correlación y logs

- **Cabecera `X-Correlation-Id`:** si el cliente la envía con un formato seguro
  (`[A-Za-z0-9._-]{1,64}`), se reutiliza; si no, el servidor genera un UUID. Se
  devuelve en toda respuesta, también en 401/403, y CORS la expone al frontend.
- El identificador se guarda en el MDC (`correlacionId`) y aparece en cada línea
  de log (`[%X{correlacionId}]`) y en el cuerpo de los errores.
- **Niveles de log:** `ERROR` con traza para 500/503; `WARN` para conflictos de
  integridad y concurrencia; `INFO` para 403; `DEBUG` para errores de negocio.
  Nunca se registran cuerpos de solicitud, contraseñas ni tokens.
- **Producción:** logs JSON estructurados (ECS, `logging.structured.format.console`)
  que incluyen el MDC.

## 7. OpenAPI

| Recurso | Ruta | Acceso |
|---|---|---|
| Documento OpenAPI 3.1 (JSON) | `/v3/api-docs` | Público en local y pruebas. En producción, deshabilitado salvo `APP_OPENAPI_HABILITADO=true`. |
| Swagger UI | `/swagger-ui.html` | Igual que el anterior. |

- Esquema de seguridad `bearerAuth` (HTTP Bearer, JWT), aplicado globalmente.
  Los endpoints públicos (login, disponibilidad, categorías) lo anulan con
  `@SecurityRequirements()`.
- `ProblemaRespuesta` y `ErrorCampo` están en `components.schemas`, y cada
  operación documenta automáticamente las respuestas 400, 401, 403, 404, 409,
  422 y 500 con ese esquema (`ConfiguracionOpenApi`).
- Solo se documentan las rutas `/api/v1/**`.
- Los DTO llevan `@Schema` con ejemplos tomados de BE-01 §10.

## 8. Consumo desde el frontend

El cliente HTTP (`apps/frontend/src/services/api/client.ts`) debe:

1. leer `codigo` y `detail` del cuerpo cuando la respuesta tenga
   `application/problem+json`, y mostrar `detail` (y `errores` junto a cada
   campo del formulario);
2. decidir el comportamiento por `codigo` (por ejemplo, redirigir al login con
   `NO_AUTENTICADO` o `SESION_INVALIDA`), nunca por el texto de `detail`;
3. enviar y registrar `X-Correlation-Id` para poder rastrear un error reportado.

Ese ajuste del frontend se especificará en su propio issue.

## 9. Verificación

- `MapeoErroresTest`: mapeo unitario de excepciones y códigos, ausencia de
  fugas en errores 500 y paginación.
- `ContratoErroresMvcTest`: MockMvc con seguridad real para 400, 401, 403, 404,
  405, 409, 422, 500 y 503, correlación y serialización de fechas y enums.
- `SeguridadBaseTest`: 401 sobre HTTP real con formato y correlación, y
  documento OpenAPI público con `bearerAuth` y `ProblemaRespuesta`.
