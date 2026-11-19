# BE-01 — API backend del MVP

| Campo | Valor |
|---|---|
| Identificador | `BE-01` |
| Estado | Propuesta para revisión del equipo |
| Contenedor | API backend (C4 Nivel 2) |
| Stack de referencia | Java 21 LTS, Spring Boot 4.1.x (la línea 3.5 perdió soporte OSS el 30-jun-2026), Spring Security, Spring Data JPA, Flyway, PostgreSQL 17 |
| Fuentes | `docs/source/contexto_proyecto_sistemas_de_informacion.pdf` (Entregable 1), `docs/source/modelo_c4.md`, `docs/source/seleccion_de_patrones_de_diseno.md`, Entregable 3 (máquina de estados del préstamo) |
| Consumidor | Aplicación web descrita en `FE-01-frontend-mvp.md` |

## 1. Objetivo

Definir el comportamiento, los contratos HTTP/JSON, los errores y los criterios
de aceptación de la API backend del MVP **antes** de crear endpoints, entidades o
migraciones. Esta especificación es la autoridad funcional de todo el trabajo en
`apps/backend/`. Cada funcionalidad del backend se implementa contra esta
especificación o contra una especificación posterior que la extienda de forma
explícita.

## 2. Alcance

**Incluye**

- Autenticación stateless y autorización por rol.
- Gestión de prestadores (usuarios con rol `PRESTADOR`).
- Gestión de categorías e inventario de unidades físicas.
- Consulta pública de disponibilidad.
- Consulta de la situación de un usuario solicitante.
- Registro y consulta de préstamos.
- Registro y consulta de devoluciones.
- Registro y consulta de incidencias.
- Cálculo de sanciones por retraso (RN-03).
- Transición automática de préstamos a vencido (RN-02).

**Excluye** (se mantiene el alcance definido en el contexto del proyecto)

- ERP, sincronización con sistemas académicos, mapas y geolocalización.
- Notificaciones externas (correo, SMS o mensajería).
- Reservas independientes del préstamo. **Préstamo y disponibilidad son los
  únicos flujos de asignación de unidades.**
- Pagos, cobros o gestión financiera de pérdidas.
- Analítica avanzada, reportes complejos y recomendaciones.
- Autoservicio de cuentas: registro público, recuperación o cambio de contraseña
  (ver `PEND-02`).
- Endpoints para crear o editar usuarios solicitantes (ver `PEND-01`).

## 3. Actores, roles y permisos

### 3.1 Modelo de usuario

Existe **una sola entidad de acceso**, `Usuario`, con un rol único:

| Rol | Actor del contexto | ¿Inicia sesión? | Descripción |
|---|---|---|---|
| `USER` | Usuario (solicitante) | No en el MVP | Estudiante, profesor o personal que recibe implementos. Es el titular del préstamo. |
| `PRESTADOR` | Prestador | Sí | Personal que registra préstamos, devoluciones e incidencias. |
| `ADMIN` | Administrador | Sí | Administra prestadores, categorías e inventario. |

Un prestador **no** es una entidad distinta: es un `Usuario` con rol
`PRESTADOR`. El recurso `/prestadores` es una vista administrativa filtrada de
`Usuario` (ver decisión `D-05`).

### 3.2 Matriz de permisos

| Operación | Público | `PRESTADOR` | `ADMIN` |
|---|:-:|:-:|:-:|
| Iniciar sesión | ✔ | — | — |
| Consultar la sesión propia | — | ✔ | ✔ |
| Consultar disponibilidad y categorías | ✔ | ✔ | ✔ |
| Crear y editar categorías | — | — | ✔ |
| Gestionar prestadores | — | — | ✔ |
| Gestionar inventario (implementos) | — | — | ✔ |
| Buscar solicitantes y consultar su situación | — | ✔ | — |
| Registrar y consultar préstamos | — | ✔ | — |
| Registrar y consultar devoluciones | — | ✔ | — |
| Registrar y consultar incidencias | — | ✔ | — |

Toda ruta no listada como pública exige autenticación. La política de seguridad
es *deny by default*: una ruta nueva queda protegida hasta que esta
especificación la declare pública (RNF-03).

## 4. Convenciones comunes de la API

### 4.1 Rutas y versión

- Prefijo obligatorio: `/api/v1`.
- Recursos en plural, en español, en minúscula y separados con guion:
  `/api/v1/prestamos`, `/api/v1/implementos`.
- Subrecursos para operaciones que pertenecen a un préstamo:
  `/api/v1/prestamos/{id}/devoluciones`.
- Los cambios de estado se exponen como subrecurso `.../estado` con `PATCH`. No
  se usan verbos en la URL.

### 4.2 Nomenclatura (español/inglés)

| Elemento | Convención | Ejemplo |
|---|---|---|
| Rutas, campos JSON, parámetros | Español, `camelCase`, **sin tildes ni ñ** | `fechaLimiteDevolucion`, `contrasena`, `tamano` |
| Valores de enumeraciones | Español, `MAYUSCULA_SNAKE`, sin tildes | `EN_MANTENIMIENTO`, `DADO_DE_BAJA` |
| Roles | Constantes técnicas en inglés | `USER`, `PRESTADOR`, `ADMIN` |
| Clases de dominio y aplicación | Español, alineado con C4 | `Prestamo`, `PrestamoServicio`, `ActualizadorDisponibilidad` |
| Sufijos y términos de framework | Inglés cuando el framework lo impone | `@RestController`, `JpaRepository`, `SecurityFilterChain` |
| Tablas y columnas | Español, `snake_case` | `detalle_prestamo`, `fecha_limite_devolucion` |
| Mensajes para el usuario (`detail`) | Español con ortografía completa | "El implemento no está disponible." |

### 4.3 Identificadores

- Todo recurso se identifica con **UUID** (versión 4, formato canónico en
  minúscula). El UUID es el único identificador válido en rutas y referencias
  (`usuarioId`, `implementoIds`).
- Los recursos que la interfaz muestra con un código legible exponen además un
  campo `codigo` **de solo lectura**, generado por el servidor:
  - préstamo: `PRE-000148`;
  - devolución: `DEV-000062`;
  - incidencia: `INC-000031`.
- El implemento tiene `codigoInventario` (por ejemplo `BAL-001`). Lo registra el
  administrador y es único. No reemplaza al UUID.

### 4.4 Fechas y horas

- Formato ISO-8601 **con desplazamiento de zona horaria**:
  `2026-08-31T18:00:00-05:00`.
- Una fecha de entrada sin desplazamiento se rechaza con `400`.
- El servidor persiste los instantes en `timestamptz` (UTC). Las respuestas se
  serializan con el desplazamiento de la zona institucional, configurable con
  `app.zona-horaria` (por defecto `America/Bogota`).
- Todas las fechas de negocio (entrega, límite, devolución, sanción) las calcula
  el servidor a partir de un `java.time.Clock` inyectado. El cliente nunca las
  envía (ver `D-07`).

### 4.5 Paginación

Los listados aceptan:

| Parámetro | Tipo | Defecto | Restricción |
|---|---|---|---|
| `pagina` | entero | `0` | ≥ 0 |
| `tamano` | entero | `20` | 1–100 |
| `orden` | `campo,asc\|desc` | depende del recurso | solo campos permitidos por cada recurso |

Respuesta paginada:

```json
{
  "contenido": [],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 0,
  "totalPaginas": 0
}
```

Un valor fuera de rango o un campo de orden no permitido produce `400`.

### 4.6 Filtros y búsqueda

- `q`: búsqueda de texto libre, sin distinguir mayúsculas ni tildes, sobre los
  campos que declara cada recurso.
- Filtros exactos con el nombre del campo: `estado`, `categoriaId`, `tipo`,
  `usuarioId`, `prestamoId`, `alcance`.
- Rangos de fecha: `desde` y `hasta` (ISO-8601 con desplazamiento; intervalo
  `[desde, hasta)`).
- Si un filtro tiene un valor que no pertenece a la enumeración, la respuesta es
  `400`.

### 4.7 Formato de respuesta

- Recursos individuales: el objeto JSON directamente, **sin envoltorio**.
- Creación: `201 Created` con el recurso creado y la cabecera `Location`.
- Cambios de estado y actualizaciones: `200 OK` con el recurso actualizado.
- `Content-Type: application/json` en respuestas exitosas y
  `application/problem+json` en errores.

### 4.8 Formato de errores

> Detalle vigente del formato, catálogo completo de códigos, correlación y
> OpenAPI: [`BE-01-A-contratos-y-errores.md`](BE-01-A-contratos-y-errores.md).

Todos los errores usan la estructura de RFC 9457 (Problem Details), extendida
con `codigo` y `errores`:

```json
{
  "type": "https://prestamos.itm.edu.co/errores/implemento-no-disponible",
  "title": "Implemento no disponible",
  "status": 409,
  "detail": "El implemento BAL-001 no está disponible para préstamo.",
  "instance": "/api/v1/prestamos",
  "codigo": "IMPLEMENTO_NO_DISPONIBLE",
  "timestamp": "2026-08-31T18:00:00-05:00",
  "errores": [
    { "campo": "implementoIds[0]", "mensaje": "El implemento BAL-001 se encuentra PRESTADO." }
  ]
}
```

- `codigo` es estable y es lo que el frontend debe usar para decidir su
  comportamiento. `detail` es un mensaje en español para la persona usuaria
  (RNF-04).
- `errores` es opcional y se usa para errores de campos o de varios elementos.
- Las respuestas de error nunca exponen trazas, SQL ni nombres de clases.

### 4.9 Cuándo se usa cada código de estado

| Código | Cuándo corresponde | Ejemplos de `codigo` |
|---|---|---|
| `400 Bad Request` | La solicitud está mal formada o no cumple la validación sintáctica del contrato: JSON inválido, campo obligatorio ausente, tipo incorrecto, UUID mal formado, fecha sin desplazamiento, enumeración desconocida, paginación fuera de rango, IDs duplicados en una misma lista. **No depende del estado de la base de datos.** | `SOLICITUD_INVALIDA`, `VALIDACION_FALLIDA` |
| `401 Unauthorized` | No hay token, el token es inválido o expiró, las credenciales son incorrectas o la cuenta del token fue desactivada. | `NO_AUTENTICADO`, `CREDENCIALES_INVALIDAS`, `SESION_INVALIDA` |
| `403 Forbidden` | El usuario está autenticado, pero su rol no permite la operación. | `ACCESO_DENEGADO` |
| `404 Not Found` | El recurso de la ruta o un recurso referenciado en el cuerpo (`usuarioId`, `implementoIds`, `categoriaId`) no existe. | `RECURSO_NO_ENCONTRADO` |
| `409 Conflict` | La operación entra en conflicto con el **estado actual** de un recurso: unicidad (código, correo, identificación), unidad no disponible o ya asignada (RN-04, RN-06), transición de estado inválida, operación sobre un préstamo finalizado, unidad ya resuelta o concurrencia. | `IMPLEMENTO_NO_DISPONIBLE`, `CONFLICTO_UNICIDAD`, `TRANSICION_INVALIDA`, `PRESTAMO_FINALIZADO`, `UNIDAD_YA_RESUELTA`, `CONFLICTO_CONCURRENCIA` |
| `422 Unprocessable Content` | La solicitud está bien formada y los recursos existen, pero una **regla de negocio sobre el solicitante o sobre la semántica de la operación** la rechaza: límite de unidades (RN-01), usuario no habilitado, suspendido o con vencidos (RN-03, RN-05), condición con novedad sin incidencia, implemento que no pertenece al préstamo, tipo de incidencia no permitido. | `LIMITE_IMPLEMENTOS_EXCEDIDO`, `USUARIO_NO_HABILITADO`, `USUARIO_SANCIONADO`, `USUARIO_CON_PRESTAMOS_VENCIDOS`, `INCIDENCIA_REQUERIDA`, `IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO`, `TIPO_INCIDENCIA_NO_PERMITIDO`, `JUSTIFICACION_REQUERIDA`, `CATEGORIA_INACTIVA` |
| `500 Internal Server Error` | Error no previsto. Se registra en el log con un identificador de correlación y se responde sin detalles internos. | `ERROR_INTERNO` |

**Regla para distinguir 409 de 422:**

- `409`: el mismo cuerpo podría tener éxito si cambia el estado de **otro
  recurso**, por ejemplo cuando la unidad vuelve a estar disponible.
- `422`: la regla se evalúa sobre la **situación del solicitante** o sobre la
  **coherencia interna de la solicitud**.

### 4.10 CORS

- Los orígenes permitidos se configuran con `app.cors.origenes-permitidos`. En
  desarrollo incluye `http://localhost:5173` (Vite).
- Solo se permiten los métodos `GET`, `POST`, `PUT` y `PATCH`, y las cabeceras
  `Authorization` y `Content-Type`.

## 5. Autenticación y sesión

| Aspecto | Decisión |
|---|---|
| Mecanismo | JWT firmado con HS256. El secreto tiene 256 bits como mínimo y se lee de `APP_JWT_SECRETO`; nunca se versiona. |
| Transporte | Cabecera `Authorization: Bearer <token>`. No se usan cookies, por lo que no se requiere protección CSRF. |
| Expiración | 60 minutos, configurable con `app.jwt.expiracion`. No hay *refresh token* en el MVP: al expirar, el usuario vuelve a iniciar sesión. |
| Claims | `sub` (UUID del usuario), `rol`, `iat`, `exp`, `ver` (versión de credenciales). |
| Almacenamiento en el cliente | En memoria o `sessionStorage`. Nunca en `localStorage`. |
| Revocación | En cada solicitud autenticada se verifica que el usuario siga `activo` y que `ver` coincida con `usuario.version_credenciales`. Desactivar un prestador o cambiar su contraseña invalida de inmediato sus tokens (`401 SESION_INVALIDA`). |
| Cierre de sesión | Lo hace el cliente descartando el token. No existe endpoint de *logout* en el MVP. |
| Contraseñas | Hash BCrypt (factor 10 como mínimo). Longitud mínima de 8 caracteres. |
| Protección de fuerza bruta | Fuera del alcance del MVP. Se registra como recomendación en `PEND-03`. |
| Rol `USER` | No tiene contraseña ni puede iniciar sesión. Si un `USER` intenta autenticarse, recibe `401 CREDENCIALES_INVALIDAS`. |

Para no revelar si una cuenta existe, el inicio de sesión devuelve la misma
respuesta (`401 CREDENCIALES_INVALIDAS`) cuando el usuario no existe, cuando la
contraseña es incorrecta, cuando la cuenta está inactiva y cuando la cuenta
tiene rol `USER`.

## 6. Modelo de dominio y estados

### 6.1 Entidades

| Entidad | Atributos principales |
|---|---|
| `Usuario` | `id`, `identificacion` (única), `nombres`, `apellidos`, `correo` (único, opcional para `USER`), `contrasenaHash` (nula para `USER`), `rol`, `activo`, `versionCredenciales`, `creadoEn`, `actualizadoEn` |
| `Categoria` | `id`, `nombre` (único), `descripcion`, `activa` |
| `ImplementoDeportivo` | `id`, `codigoInventario` (único), `nombre`, `descripcion`, `categoria`, `estado`, `fechaRegistro`, `version` (bloqueo optimista) |
| `Prestamo` | `id`, `codigo`, `usuario` (`USER`), `prestador`, `fechaPrestamo`, `fechaLimiteDevolucion`, `fechaFinalizacion`, `estado`, `detalles` |
| `DetallePrestamo` | `id`, `prestamo`, `implemento`, `estado` |
| `Devolucion` | `id`, `codigo`, `prestamo`, `prestador`, `fechaDevolucion`, `retrasoJustificado`, `justificacion`, `alcance`, `detalles` |
| `DetalleDevolucion` | `id`, `devolucion`, `detallePrestamo`, `condicion`, `observaciones` |
| `Incidencia` | `id`, `codigo`, `detallePrestamo`, `prestador`, `tipo`, `descripcion`, `fechaRegistro`, `estado` |
| `Sancion` | `id`, `usuario`, `prestamo`, `devolucion`, `fechaInicio`, `fechaFin`, `minutosRetraso`, `estado` |

### 6.2 Estado del implemento (patrón State)

| Estado | ¿Prestable? | Significado |
|---|:-:|---|
| `DISPONIBLE` | ✔ | Apto y libre. |
| `PRESTADO` | ✘ | Asociado a un `DetallePrestamo` en `PENDIENTE_DEVOLUCION`. |
| `EN_MANTENIMIENTO` | ✘ | Fuera de servicio temporal: daño, otra novedad o decisión administrativa. |
| `DADO_DE_BAJA` | ✘ | Retirado definitivamente: baja administrativa, pérdida o robo. Es un estado terminal. |

Transiciones permitidas (cualquier otra lanza `TransicionInvalidaException` y se
traduce a `409 TRANSICION_INVALIDA`):

| Desde | Evento | Hacia | Quién |
|---|---|---|---|
| `DISPONIBLE` | prestar | `PRESTADO` | Sistema (registro de préstamo) |
| `PRESTADO` | devolver en buen estado | `DISPONIBLE` | Sistema (devolución) |
| `PRESTADO` | devolver con novedad (`DANO`/`OTRA`) | `EN_MANTENIMIENTO` | Sistema (devolución + incidencia) |
| `PRESTADO` | incidencia `PERDIDA`/`ROBO` | `DADO_DE_BAJA` | Sistema (incidencia) |
| `DISPONIBLE` | enviar a mantenimiento | `EN_MANTENIMIENTO` | `ADMIN` |
| `EN_MANTENIMIENTO` | habilitar | `DISPONIBLE` | `ADMIN` |
| `DISPONIBLE`, `EN_MANTENIMIENTO` | dar de baja | `DADO_DE_BAJA` | `ADMIN` |

El administrador nunca puede cambiar manualmente el estado de una unidad
`PRESTADO`.

### 6.3 Estado del préstamo (máquina de estados del Entregable 3)

| Estado | Significado | Se muestra en la UI como |
|---|---|---|
| `VIGENTE` | Dentro del plazo y sin unidades resueltas. | Activo |
| `DEVOLUCION_PARCIAL` | Dentro del plazo; al menos una unidad resuelta y otras pendientes. | Activo |
| `VENCIDO` | Superó `fechaLimiteDevolucion` con unidades pendientes. | Vencido |
| `FINALIZADO` | Todas las unidades se devolvieron o quedaron resueltas por incidencia (RN-07). Es un estado terminal. | Finalizado |

| Desde | Evento | Guarda | Hacia |
|---|---|---|---|
| (inicial) | `registrarEntrega` | Cadena de elegibilidad aprobada | `VIGENTE` |
| `VIGENTE` | devolución o incidencia | Quedan unidades pendientes | `DEVOLUCION_PARCIAL` |
| `VIGENTE`, `DEVOLUCION_PARCIAL`, `VENCIDO` | devolución o incidencia | **No** quedan unidades pendientes (RN-07) | `FINALIZADO` |
| `DEVOLUCION_PARCIAL` | devolución o incidencia | Quedan unidades pendientes | `DEVOLUCION_PARCIAL` (transición interna) |
| `VENCIDO` | devolución o incidencia | Quedan unidades pendientes | `VENCIDO` (transición interna) |
| `VIGENTE`, `DEVOLUCION_PARCIAL` | `after(24 h)` | Ahora > `fechaLimiteDevolucion` y quedan unidades pendientes | `VENCIDO` |

La guarda de cierre depende **solo** de RN-07. La sanción de RN-03 es una
**acción** de la devolución tardía y no una condición para cerrar el préstamo
(ver `D-08`). No existe un endpoint de cierre manual: el cierre lo evalúa
`PoliticaCierrePrestamo` después de cada devolución o incidencia.

### 6.4 Otros estados y enumeraciones

| Enumeración | Valores |
|---|---|
| `EstadoDetallePrestamo` | `PENDIENTE_DEVOLUCION`, `DEVUELTO`, `RESUELTO_POR_INCIDENCIA` |
| `CondicionDevolucion` | `BUENO`, `CON_NOVEDAD` |
| `TipoIncidencia` | `DANO`, `PERDIDA`, `ROBO`, `OTRA` |
| `EstadoIncidencia` | `REGISTRADA`, `RESUELTA`. En el MVP solo se crea `REGISTRADA`; ver `PEND-04`. |
| `AlcanceDevolucion` | `PARCIAL` (quedan unidades pendientes después de la devolución) o `COMPLETA` (la devolución resuelve las últimas pendientes) |
| `EstadoSancion` | `ACTIVA`, `CUMPLIDA` |

## 7. Reglas de negocio aplicadas

| RN | Regla operativa en la API | Dónde se aplica | Error |
|---|---|---|---|
| RN-01 | La suma de unidades `PENDIENTE_DEVOLUCION` del usuario más las unidades solicitadas no puede superar **4** (ver `D-01`). | `ValidadorLimitePrestamo` | `422 LIMITE_IMPLEMENTOS_EXCEDIDO` |
| RN-02 | `fechaLimiteDevolucion = fechaPrestamo + 24 h`. La calcula el servidor. | `ValidadorFechas`, `Prestamo.crear` | — |
| RN-03 | En una devolución tardía (`fechaDevolucion > fechaLimiteDevolucion`) sin justificación, se crea una `Sancion` con duración `max(48 h, 2 × retraso)`. Si el retraso está justificado, no hay sanción. | `PoliticaSancion` (Strategy) en `DevolucionServicio` | — |
| RN-04 | Solo se pueden prestar unidades en `DISPONIBLE`. | `ValidadorDisponibilidad`, estado del implemento | `409 IMPLEMENTO_NO_DISPONIBLE` |
| RN-05 | El solicitante debe tener rol `USER`, estar `activo`, no tener una sanción `ACTIVA` vigente y no tener préstamos vencidos con unidades pendientes. | `ValidadorUsuarioHabilitado` | `422 USUARIO_NO_HABILITADO`, `422 USUARIO_SANCIONADO`, `422 USUARIO_CON_PRESTAMOS_VENCIDOS` |
| RN-06 | Una unidad solo puede tener un `DetallePrestamo` en `PENDIENTE_DEVOLUCION`. Se garantiza con un bloqueo `SELECT … FOR UPDATE` sobre las unidades y con un índice único parcial en la base de datos. | `ActualizadorDisponibilidad`, PostgreSQL | `409 IMPLEMENTO_NO_DISPONIBLE` o `409 CONFLICTO_CONCURRENCIA` |
| RN-07 | El préstamo pasa a `FINALIZADO` cuando ningún detalle queda en `PENDIENTE_DEVOLUCION`. | `PoliticaCierrePrestamo` | — |

**Vencimiento y elegibilidad.** La elegibilidad (RN-05) no depende de que la
tarea programada ya haya marcado el préstamo como `VENCIDO`. La consulta evalúa
directamente `fechaLimiteDevolucion < ahora` con unidades pendientes. La tarea
programada (CU-20) solo sincroniza el estado persistido (ver `D-09`).

**Cálculo de la sanción.**

- `retraso = fechaDevolucion − fechaLimiteDevolucion`.
- `duracion = max(48 h, 2 × retraso)`.
- `fechaInicio = fechaDevolucion` y `fechaFin = fechaInicio + duracion`.
- Si el usuario ya tiene sanciones activas, la suspensión efectiva termina en la
  mayor `fechaFin`.
- Una sanción pasa a `CUMPLIDA` cuando `ahora ≥ fechaFin`. Esta condición se
  evalúa en cada consulta de situación y en la tarea programada.

## 8. Casos de uso

Formato: actor, trazabilidad, flujo principal (FP) y flujos alternos (FA). Los
flujos alternos de autenticación y autorización (`401`/`403`) aplican a todos
los casos protegidos y no se repiten.

### CU-01 Iniciar sesión
- **Actor:** Administrador o Prestador. **Traza:** RU-01, HU-01, RF-01, RF-02.
- **FP:**
  1. El actor envía su identificador (correo o identificación) y su contraseña.
  2. El sistema valida las credenciales y que la cuenta esté activa.
  3. El sistema emite un JWT con el rol del usuario.
- **FA1:** credenciales inválidas, cuenta inactiva o rol `USER` → `401 CREDENCIALES_INVALIDAS`.
- **FA2:** campos vacíos → `400 VALIDACION_FALLIDA`.

### CU-02 Consultar sesión actual
- **Actor:** Administrador o Prestador. **Traza:** RF-02.
- **FP:** el sistema devuelve el usuario autenticado y su rol, para que la
  interfaz muestre solo las funciones de ese rol.

### CU-03 Registrar prestador
- **Actor:** Administrador. **Traza:** RU-02, HU-02, RF-03.
- **FP:**
  1. El administrador envía nombres, apellidos, identificación, correo y
     contraseña inicial.
  2. El sistema crea un `Usuario` con rol `PRESTADOR` y `activo = true`.
- **FA1:** faltan campos obligatorios → `400 VALIDACION_FALLIDA`, con la lista
  de campos a completar en `errores`.
- **FA2:** el correo o la identificación ya existen → `409 CONFLICTO_UNICIDAD`.

### CU-04 Consultar y buscar prestadores
- **Actor:** Administrador. **Traza:** RU-03, HU-03, RF-04, RF-05.
- **FP:** el sistema lista los prestadores, paginados y filtrables por `q`
  (nombre, identificación o correo) y por `estado` (`ACTIVO`/`INACTIVO`).
- **FA1:** no hay coincidencias → `200` con `contenido` vacío.

### CU-05 Actualizar prestador
- **Actor:** Administrador. **Traza:** RU-04, HU-04 (escenario 1), RF-06.
- **FP:** el sistema actualiza nombres, apellidos, identificación y correo, y
  devuelve el prestador actualizado.
- **FA1:** el prestador no existe → `404`.
- **FA2:** se intenta usar un correo o una identificación de otro usuario →
  `409 CONFLICTO_UNICIDAD`.

### CU-06 Activar o desactivar prestador
- **Actor:** Administrador. **Traza:** RU-04, HU-04 (escenario 2), RF-06.
- **FP:**
  1. El sistema cambia `activo` y conserva el historial.
  2. Al desactivar, incrementa `versionCredenciales`, lo que invalida los tokens
     emitidos.
- **FA1:** el ID no corresponde a un usuario con rol `PRESTADOR` → `404`.

### CU-07 Gestionar categorías
- **Actor:** Administrador (crear y editar); público (consultar). **Traza:**
  RF-07, RF-10 y funcionalidad F6 (soporte de la categoría obligatoria del
  implemento y del filtro de disponibilidad).
- **FP:** crear una categoría, editar su nombre o descripción, y activarla o
  desactivarla.
- **FA1:** el nombre está duplicado → `409 CONFLICTO_UNICIDAD`.
- **Regla:** una categoría inactiva no admite implementos nuevos, pero conserva
  los existentes.

### CU-08 Registrar implemento
- **Actor:** Administrador. **Traza:** RU-05, HU-05, RF-07, RN-04.
- **FP:** el administrador envía código de inventario, nombre, categoría,
  descripción opcional y estado inicial (`DISPONIBLE` por defecto o
  `EN_MANTENIMIENTO`). El sistema crea la unidad física.
- **FA1:** faltan datos → `400`.
- **FA2:** la categoría no existe → `404`; la categoría está inactiva → `422 CATEGORIA_INACTIVA`.
- **FA3:** el código de inventario está duplicado → `409 CONFLICTO_UNICIDAD`.
- **FA4:** el estado inicial es `PRESTADO` o `DADO_DE_BAJA` → `400 VALIDACION_FALLIDA`.

### CU-09 Consultar y buscar implementos
- **Actor:** Administrador. **Traza:** RU-06, HU-06 (escenario 1), RF-08.
- **FP:** listado paginado con filtros `q` (código o nombre), `categoriaId` y
  `estado`, más un resumen de conteos por estado.

### CU-10 Actualizar implemento
- **Actor:** Administrador. **Traza:** RU-06, HU-06 (escenario 2), RF-08.
- **FP:** el sistema actualiza código de inventario, nombre, descripción y
  categoría. **El estado no se modifica por esta operación** (ver CU-11).
- **FA1:** el código está duplicado → `409`.

### CU-11 Cambiar estado o retirar implemento
- **Actor:** Administrador. **Traza:** RU-07, HU-07, RF-09, RN-04.
- **FP:** el administrador solicita el nuevo estado (`DISPONIBLE`,
  `EN_MANTENIMIENTO` o `DADO_DE_BAJA`) y un motivo. El sistema aplica la
  transición mediante el patrón State. "Retirar de circulación" equivale a
  `EN_MANTENIMIENTO` (temporal) o a `DADO_DE_BAJA` (definitivo).
- **FA1:** la unidad está `PRESTADO`, ya está `DADO_DE_BAJA` o la transición no
  es válida → `409 TRANSICION_INVALIDA`.
- **Regla:** no existe borrado físico de implementos, para conservar la
  trazabilidad.

### CU-12 Consultar disponibilidad (público)
- **Actor:** Usuario sin autenticar. **Traza:** RU-08, HU-08, RF-10, RN-04.
- **FP:**
  1. El sistema agrupa las unidades no dadas de baja por (`categoria`, `nombre`).
  2. Devuelve las unidades disponibles y el total. Admite filtros `q` y
     `categoriaId`.
- **FA1:** no hay coincidencias → `200` con `contenido` vacío. La interfaz
  muestra "no existen implementos disponibles" (HU-08, escenario 2).
- **Regla:** la respuesta pública no expone códigos de inventario, UUID de
  unidades ni datos de préstamos.

### CU-13 Buscar solicitante y consultar su situación
- **Actor:** Prestador. **Traza:** RU-11, HU-11, RF-12, RF-14, RN-01, RN-02,
  RN-03, RN-05.
- **FP:**
  1. El prestador busca por nombre o identificación (solo rol `USER`).
  2. Para el usuario elegido, el sistema devuelve si está habilitado, sus
     préstamos activos, las unidades activas, las unidades que aún puede recibir
     (`4 − activas`), los préstamos vencidos, la sanción vigente y los motivos de
     inhabilitación.
- **FA1:** el usuario no existe o no tiene rol `USER` → `404`.
- **Regla:** esta consulta **no falla** cuando el usuario está inhabilitado:
  responde `200` con `habilitado: false` y la lista de `motivos`.

### CU-14 Registrar préstamo
- **Actor:** Prestador. **Traza:** RU-09, RU-12, HU-09, HU-12, RF-11, RF-12,
  RF-13, RN-01, RN-02, RN-04, RN-05, RN-06. **Patrones:** Facade
  (`PrestamoServicio`), Chain of Responsibility (`PoliticaElegibilidadPrestamo`),
  Mediator (`ActualizadorDisponibilidad`), State (`ImplementoDeportivo`).
- **FP:**
  1. El prestador envía `usuarioId` y `implementoIds` (de 1 a 4, sin repetidos).
  2. En una sola transacción, el sistema:
     1. bloquea las unidades;
     2. ejecuta la cadena de validación en este orden: usuario habilitado →
        límite → fechas → disponibilidad;
     3. crea el préstamo en estado `VIGENTE` con
        `fechaLimiteDevolucion = ahora + 24 h`;
     4. marca las unidades como `PRESTADO`.
  3. El sistema devuelve `201`.
- **FA1:** el usuario o algún implemento no existe → `404`, indicando cuál.
- **FA2:** IDs repetidos o una lista vacía o con más de 4 elementos → `400`.
- **FA3:** usuario inactivo, sancionado o con préstamos vencidos → `422` con el
  código específico.
- **FA4:** se excede el límite de 4 unidades activas → `422 LIMITE_IMPLEMENTOS_EXCEDIDO`.
- **FA5:** alguna unidad no está `DISPONIBLE` → `409 IMPLEMENTO_NO_DISPONIBLE`,
  indicando cada unidad en `errores` (HU-09, escenario 2).
- **FA6:** dos prestadores intentan prestar la misma unidad al mismo tiempo → una
  operación tiene éxito y la otra recibe `409` (HU-12, escenario 2).
- **Garantía:** si una validación falla, no se persiste ningún cambio (RNF-05).

### CU-15 Consultar préstamos
- **Actor:** Prestador. **Traza:** RU-10, HU-10, RF-14, RN-07.
- **FP:** listado paginado con filtros `q` (código del préstamo, nombre o
  identificación del usuario), `estado` (admite varios valores separados por
  coma), `usuarioId`, `desde` y `hasta` sobre `fechaPrestamo`. Hay además un
  detalle por ID con sus unidades, devoluciones e incidencias.
- **Orden por defecto:** `fechaPrestamo,desc`.

### CU-16 Registrar devolución
- **Actor:** Prestador. **Traza:** RU-13, RU-16, HU-13, HU-16, RF-15, RF-16,
  RF-18, RN-03, RN-06, RN-07. **Patrones:** Strategy (`PoliticaSancion`),
  Mediator, State.
- **FP:**
  1. El prestador envía las unidades que regresan, cada una con su `condicion` y
     sus observaciones. Si alguna llegó tarde, indica `retrasoJustificado` y la
     `justificacion`.
  2. En una transacción, el sistema registra la `Devolucion` y sus detalles con
     `fechaDevolucion = ahora`.
  3. Por cada unidad:
     - con condición `BUENO`: la unidad pasa a `DISPONIBLE` y el detalle a
       `DEVUELTO`;
     - con condición `CON_NOVEDAD`: se crea la incidencia que acompaña al ítem,
       la unidad pasa a `EN_MANTENIMIENTO` y el detalle a `DEVUELTO`.
  4. Si la devolución es tardía, el sistema aplica `PoliticaSancion`.
  5. El sistema evalúa el cierre del préstamo (RN-07), calcula el `alcance` y
     devuelve `201`.
- **FA1:** el préstamo no existe → `404`.
- **FA2:** el préstamo está `FINALIZADO` → `409 PRESTAMO_FINALIZADO`.
- **FA3:** la unidad no pertenece al préstamo → `422 IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO`.
- **FA4:** la unidad ya fue devuelta o resuelta → `409 UNIDAD_YA_RESUELTA`.
- **FA5:** condición `CON_NOVEDAD` sin `incidencia` → `422 INCIDENCIA_REQUERIDA`
  (HU-13, escenario 2).
- **FA6:** incidencia de tipo `PERDIDA` o `ROBO` dentro de una devolución (la
  unidad sí regresó) → `422 TIPO_INCIDENCIA_NO_PERMITIDO`.
- **FA7:** `retrasoJustificado = true` sin `justificacion` → `422 JUSTIFICACION_REQUERIDA`.
- **FA8:** la lista de ítems está vacía o tiene unidades repetidas → `400`.

### CU-17 Consultar devoluciones
- **Actor:** Prestador. **Traza:** RU-14, HU-14 (ver `D-02` sobre RF-19).
- **FP:** listado paginado con préstamo relacionado, usuario, unidades,
  condición, alcance y fecha. Se puede filtrar por `q` (código de devolución o
  de préstamo, nombre o identificación del usuario), `prestamoId`, `usuarioId`,
  `alcance`, `desde` y `hasta`. También hay un detalle por ID.
- **Orden por defecto:** `fechaDevolucion,desc`.

### CU-18 Registrar incidencia sin devolución
- **Actor:** Prestador. **Traza:** RU-15, RU-16, HU-15 (escenario 2), HU-16,
  RF-17, RF-18, RN-06, RN-07.
- **FP:**
  1. Para una unidad pendiente que el usuario reporta como perdida o robada, el
     prestador registra la incidencia con su tipo y descripción.
  2. La unidad pasa a `DADO_DE_BAJA` y el detalle a `RESUELTO_POR_INCIDENCIA`.
  3. El sistema evalúa el cierre del préstamo.
- **FA1:** el tipo es `DANO` u `OTRA` → `422 TIPO_INCIDENCIA_NO_PERMITIDO`. Un
  daño se registra con la devolución, porque la unidad regresa (CU-16).
- **FA2 a FA4:** préstamo finalizado (`409`), unidad que no pertenece al
  préstamo (`422`) y unidad ya resuelta (`409`), igual que en CU-16.
- **Regla:** una incidencia no genera sanción por retraso. Las consecuencias
  económicas quedan fuera del alcance.

### CU-19 Consultar incidencias
- **Actor:** Prestador. **Traza:** RU-15, HU-15, RF-17 (consulta del historial
  que registra el RF), en soporte de la vista "Incidencias" de FE-01.
- **FP:** listado paginado con filtros `q` (código de incidencia, código de
  inventario o usuario), `tipo`, `prestamoId`, `implementoId`, `desde` y
  `hasta`. También hay un detalle por ID.

### CU-20 Marcar préstamos vencidos (sistema)
- **Actor:** Sistema. **Traza:** RN-02, RN-05, transición `after(24 h)` del
  Entregable 3.
- **FP:**
  1. Una tarea programada (`@Scheduled`, cada 5 minutos, configurable) cambia a
     `VENCIDO` los préstamos en `VIGENTE` o `DEVOLUCION_PARCIAL` cuya
     `fechaLimiteDevolucion < ahora`.
  2. La misma tarea marca como `CUMPLIDA` las sanciones cuya `fechaFin` ya pasó.
- **Regla:** la tarea es idempotente. Además, las consultas y las validaciones
  aplican la misma condición al leer, por lo que el resultado no depende de la
  frecuencia de la tarea.

## 9. Catálogo de endpoints

| # | Método y ruta | Rol | CU | Respuesta |
|---|---|---|---|---|
| 1 | `POST /api/v1/auth/login` | Público | CU-01 | `200` |
| 2 | `GET /api/v1/auth/me` | `ADMIN`, `PRESTADOR` | CU-02 | `200` |
| 3 | `GET /api/v1/disponibilidad` | Público | CU-12 | `200` paginado |
| 4 | `GET /api/v1/categorias` | Público | CU-07 | `200` (lista sin paginar) |
| 5 | `POST /api/v1/categorias` | `ADMIN` | CU-07 | `201` |
| 6 | `PUT /api/v1/categorias/{id}` | `ADMIN` | CU-07 | `200` |
| 7 | `GET /api/v1/prestadores` | `ADMIN` | CU-04 | `200` paginado |
| 8 | `POST /api/v1/prestadores` | `ADMIN` | CU-03 | `201` |
| 9 | `GET /api/v1/prestadores/{id}` | `ADMIN` | CU-04 | `200` |
| 10 | `PUT /api/v1/prestadores/{id}` | `ADMIN` | CU-05 | `200` |
| 11 | `PATCH /api/v1/prestadores/{id}/estado` | `ADMIN` | CU-06 | `200` |
| 12 | `GET /api/v1/implementos` | `ADMIN` | CU-09 | `200` paginado + resumen |
| 13 | `POST /api/v1/implementos` | `ADMIN` | CU-08 | `201` |
| 14 | `GET /api/v1/implementos/{id}` | `ADMIN` | CU-09 | `200` |
| 15 | `PUT /api/v1/implementos/{id}` | `ADMIN` | CU-10 | `200` |
| 16 | `PATCH /api/v1/implementos/{id}/estado` | `ADMIN` | CU-11 | `200` |
| 17 | `GET /api/v1/usuarios` | `PRESTADOR` | CU-13 | `200` paginado (solo rol `USER`) |
| 18 | `GET /api/v1/usuarios/{id}/situacion` | `PRESTADOR` | CU-13 | `200` |
| 19 | `GET /api/v1/implementos/prestables` | `PRESTADOR` | CU-14 | `200` paginado (unidades `DISPONIBLE`) |
| 20 | `POST /api/v1/prestamos` | `PRESTADOR` | CU-14 | `201` |
| 21 | `GET /api/v1/prestamos` | `PRESTADOR` | CU-15 | `200` paginado |
| 22 | `GET /api/v1/prestamos/{id}` | `PRESTADOR` | CU-15 | `200` |
| 23 | `POST /api/v1/prestamos/{id}/devoluciones` | `PRESTADOR` | CU-16 | `201` |
| 24 | `GET /api/v1/devoluciones` | `PRESTADOR` | CU-17 | `200` paginado |
| 25 | `GET /api/v1/devoluciones/{id}` | `PRESTADOR` | CU-17 | `200` |
| 26 | `POST /api/v1/prestamos/{id}/incidencias` | `PRESTADOR` | CU-18 | `201` |
| 27 | `GET /api/v1/incidencias` | `PRESTADOR` | CU-19 | `200` paginado |
| 28 | `GET /api/v1/incidencias/{id}` | `PRESTADOR` | CU-19 | `200` |
| — | `GET /actuator/health` | Público (solo estado) | RNF-02 | `200`/`503` |

El endpoint 19 existe porque el prestador necesita seleccionar **unidades
físicas** con su UUID al registrar el préstamo (paso 2 del asistente de FE-01).
La consulta pública (3) agrupa las unidades y no expone identificadores. El
endpoint 19 admite `q` y `categoriaId`.

## 10. Contratos y ejemplos

Los ejemplos son normativos en estructura y nombres de campo. Los valores son
ilustrativos.

### 10.1 Autenticación

`POST /api/v1/auth/login`

```json
{ "identificador": "maria.rodriguez@institucion.edu.co", "contrasena": "S3gura!2026" }
```

`200 OK`

```json
{
  "tokenAcceso": "eyJhbGciOiJIUzI1NiJ9...",
  "tipoToken": "Bearer",
  "expiraEn": "2026-08-31T19:00:00-05:00",
  "usuario": {
    "id": "6f1c2a4e-3b8d-4c2a-9e51-0a7d3f2b1c11",
    "nombres": "María",
    "apellidos": "Rodríguez",
    "correo": "maria.rodriguez@institucion.edu.co",
    "rol": "PRESTADOR"
  }
}
```

`401 Unauthorized`

```json
{
  "type": "https://prestamos.itm.edu.co/errores/credenciales-invalidas",
  "title": "Credenciales inválidas",
  "status": 401,
  "detail": "El usuario o la contraseña no son válidos.",
  "instance": "/api/v1/auth/login",
  "codigo": "CREDENCIALES_INVALIDAS",
  "timestamp": "2026-08-31T18:00:00-05:00"
}
```

`GET /api/v1/auth/me` devuelve `200` con el mismo objeto `usuario` de la
respuesta de login.

### 10.2 Disponibilidad (público)

`GET /api/v1/disponibilidad?categoriaId=2b7e…&q=balon&pagina=0&tamano=20`

`200 OK`

```json
{
  "contenido": [
    {
      "nombre": "Balón de fútbol",
      "categoria": { "id": "2b7e9d10-55aa-4c1e-8f0b-6a3c9e7d2f01", "nombre": "Balones" },
      "unidadesDisponibles": 3,
      "unidadesTotales": 5,
      "disponible": true
    },
    {
      "nombre": "Balón de baloncesto",
      "categoria": { "id": "2b7e9d10-55aa-4c1e-8f0b-6a3c9e7d2f01", "nombre": "Balones" },
      "unidadesDisponibles": 0,
      "unidadesTotales": 2,
      "disponible": false
    }
  ],
  "pagina": 0,
  "tamano": 20,
  "totalElementos": 2,
  "totalPaginas": 1
}
```

- `unidadesTotales` excluye las unidades `DADO_DE_BAJA`.
- `disponible = unidadesDisponibles > 0`.
- Orden por defecto: `nombre,asc`.

`GET /api/v1/categorias` → `200`:
`[{ "id": "2b7e…", "nombre": "Balones", "descripcion": null, "activa": true }]`.
Los usuarios anónimos solo reciben las categorías activas.

### 10.3 Prestadores

`POST /api/v1/prestadores`

```json
{
  "nombres": "Felipe",
  "apellidos": "Gómez",
  "identificacion": "1045678901",
  "correo": "felipe.gomez@institucion.edu.co",
  "contrasenaInicial": "Temporal#2026"
}
```

`201 Created` (`Location: /api/v1/prestadores/9a0e…`)

```json
{
  "id": "9a0e4c7b-1d2f-4e8a-b3c5-7f6d5e4c3b2a",
  "nombres": "Felipe",
  "apellidos": "Gómez",
  "identificacion": "1045678901",
  "correo": "felipe.gomez@institucion.edu.co",
  "rol": "PRESTADOR",
  "estado": "ACTIVO",
  "creadoEn": "2026-08-31T10:15:00-05:00"
}
```

- `GET /api/v1/prestadores?q=gomez&estado=ACTIVO` devuelve una página de este
  mismo objeto. El campo `estado` es una vista de `activo` (`ACTIVO`/`INACTIVO`).
- `PUT /api/v1/prestadores/{id}` recibe nombres, apellidos, identificación y
  correo (sin contraseña) y devuelve `200` con el objeto.
- `PATCH /api/v1/prestadores/{id}/estado` recibe `{ "estado": "INACTIVO" }` y
  devuelve `200`.

### 10.4 Inventario

`POST /api/v1/implementos`

```json
{
  "codigoInventario": "BAL-003",
  "nombre": "Balón de baloncesto",
  "categoriaId": "2b7e9d10-55aa-4c1e-8f0b-6a3c9e7d2f01",
  "descripcion": "Talla 7",
  "estado": "DISPONIBLE"
}
```

`201 Created`

```json
{
  "id": "c3d4e5f6-0718-4293-a4b5-c6d7e8f90a1b",
  "codigoInventario": "BAL-003",
  "nombre": "Balón de baloncesto",
  "descripcion": "Talla 7",
  "categoria": { "id": "2b7e9d10-55aa-4c1e-8f0b-6a3c9e7d2f01", "nombre": "Balones" },
  "estado": "DISPONIBLE",
  "prestable": true,
  "fechaRegistro": "2026-08-31T10:20:00-05:00"
}
```

`GET /api/v1/implementos?q=bal&categoriaId=…&estado=DISPONIBLE` → `200`:

```json
{
  "contenido": [ { "id": "c3d4…", "codigoInventario": "BAL-003", "nombre": "Balón de baloncesto", "descripcion": "Talla 7", "categoria": { "id": "2b7e…", "nombre": "Balones" }, "estado": "DISPONIBLE", "prestable": true, "fechaRegistro": "2026-08-31T10:20:00-05:00" } ],
  "pagina": 0, "tamano": 20, "totalElementos": 1, "totalPaginas": 1,
  "resumen": { "total": 100, "DISPONIBLE": 80, "PRESTADO": 6, "EN_MANTENIMIENTO": 10, "DADO_DE_BAJA": 4 }
}
```

`resumen` se calcula sobre todo el inventario y no depende de los filtros.

`PATCH /api/v1/implementos/{id}/estado`

```json
{ "estado": "DADO_DE_BAJA", "motivo": "Desgaste irreparable" }
```

`409 Conflict`, cuando la unidad está `PRESTADO`:

```json
{
  "type": "https://prestamos.itm.edu.co/errores/transicion-invalida",
  "title": "Transición de estado no permitida",
  "status": 409,
  "detail": "El implemento BAL-001 está prestado y no puede cambiar de estado manualmente.",
  "instance": "/api/v1/implementos/0b1c…/estado",
  "codigo": "TRANSICION_INVALIDA",
  "timestamp": "2026-08-31T18:05:00-05:00"
}
```

### 10.5 Situación del solicitante

`GET /api/v1/usuarios?q=102334455` → página de
`{ "id", "nombres", "apellidos", "identificacion", "activo" }`.

`GET /api/v1/usuarios/{id}/situacion` → `200 OK`

```json
{
  "usuario": { "id": "1d2e3f40-5a6b-4c7d-8e9f-0a1b2c3d4e5f", "nombres": "Laura", "apellidos": "Gómez", "identificacion": "102334455" },
  "habilitado": true,
  "motivos": [],
  "unidadesActivas": 1,
  "unidadesMaximas": 4,
  "unidadesDisponiblesParaPrestar": 3,
  "prestamosActivos": 1,
  "prestamosVencidos": 0,
  "sancionVigente": null
}
```

Ejemplo de usuario inhabilitado:

```json
{
  "habilitado": false,
  "motivos": [
    { "codigo": "USUARIO_CON_PRESTAMOS_VENCIDOS", "mensaje": "Tiene 1 préstamo vencido pendiente de devolución." },
    { "codigo": "USUARIO_SANCIONADO", "mensaje": "Suspendido hasta el 4 sep 2026, 18:00." }
  ],
  "sancionVigente": { "fechaInicio": "2026-08-31T18:00:00-05:00", "fechaFin": "2026-09-04T18:00:00-05:00" }
}
```

En este ejemplo se omiten los campos que no cambian respecto al anterior.

### 10.6 Préstamos

`POST /api/v1/prestamos`

```json
{
  "usuarioId": "1d2e3f40-5a6b-4c7d-8e9f-0a1b2c3d4e5f",
  "implementoIds": [
    "c3d4e5f6-0718-4293-a4b5-c6d7e8f90a1b",
    "d4e5f607-1829-4a3b-b5c6-d7e8f90a1b2c"
  ]
}
```

`201 Created` (`Location: /api/v1/prestamos/7e8f…`)

```json
{
  "id": "7e8f9a0b-1c2d-4e3f-a4b5-c6d7e8f9a0b1",
  "codigo": "PRE-000149",
  "estado": "VIGENTE",
  "usuario": { "id": "1d2e…", "nombres": "Laura", "apellidos": "Gómez", "identificacion": "102334455" },
  "prestador": { "id": "6f1c…", "nombres": "María", "apellidos": "Rodríguez" },
  "fechaPrestamo": "2026-08-31T18:00:00-05:00",
  "fechaLimiteDevolucion": "2026-09-01T18:00:00-05:00",
  "fechaFinalizacion": null,
  "vencido": false,
  "unidadesPendientes": 2,
  "detalles": [
    { "id": "aa11…", "implemento": { "id": "c3d4…", "codigoInventario": "BAL-001", "nombre": "Balón de fútbol" }, "estado": "PENDIENTE_DEVOLUCION" },
    { "id": "bb22…", "implemento": { "id": "d4e5…", "codigoInventario": "PET-014", "nombre": "Petos deportivos" }, "estado": "PENDIENTE_DEVOLUCION" }
  ]
}
```

`422 Unprocessable Content`, por límite excedido:

```json
{
  "type": "https://prestamos.itm.edu.co/errores/limite-implementos-excedido",
  "title": "Límite de implementos excedido",
  "status": 422,
  "detail": "El usuario tiene 3 implementos activos y solo puede recibir 1 más (máximo 4).",
  "instance": "/api/v1/prestamos",
  "codigo": "LIMITE_IMPLEMENTOS_EXCEDIDO",
  "timestamp": "2026-08-31T18:00:00-05:00"
}
```

`409 Conflict`, por unidad no disponible: ver el ejemplo de §4.8.

- `GET /api/v1/prestamos?estado=VIGENTE,DEVOLUCION_PARCIAL&q=gomez` devuelve
  una página con el mismo objeto, sin `detalles` y con `cantidadImplementos`.
- `GET /api/v1/prestamos/{id}` devuelve el objeto completo más `devoluciones`
  (resumen: `id`, `codigo`, `fechaDevolucion`) e `incidencias` (resumen: `id`,
  `codigo`, `tipo`, `codigoInventario`).

### 10.7 Devoluciones

`POST /api/v1/prestamos/{id}/devoluciones`

```json
{
  "items": [
    { "implementoId": "c3d4e5f6-0718-4293-a4b5-c6d7e8f90a1b", "condicion": "BUENO", "observaciones": null },
    {
      "implementoId": "d4e5f607-1829-4a3b-b5c6-d7e8f90a1b2c",
      "condicion": "CON_NOVEDAD",
      "observaciones": "Costura rota",
      "incidencia": { "tipo": "DANO", "descripcion": "Peto con costura lateral rota." }
    }
  ],
  "retrasoJustificado": false,
  "justificacion": null
}
```

`201 Created`

```json
{
  "id": "e5f60718-293a-4b4c-8d5e-6f708192a3b4",
  "codigo": "DEV-000063",
  "prestamo": { "id": "7e8f…", "codigo": "PRE-000149", "estado": "FINALIZADO" },
  "usuario": { "id": "1d2e…", "nombres": "Laura", "apellidos": "Gómez", "identificacion": "102334455" },
  "fechaDevolucion": "2026-09-01T20:00:00-05:00",
  "alcance": "COMPLETA",
  "retrasoJustificado": false,
  "items": [
    { "implemento": { "id": "c3d4…", "codigoInventario": "BAL-001", "nombre": "Balón de fútbol" }, "condicion": "BUENO", "estadoImplementoResultante": "DISPONIBLE", "incidencia": null },
    { "implemento": { "id": "d4e5…", "codigoInventario": "PET-014", "nombre": "Petos deportivos" }, "condicion": "CON_NOVEDAD", "estadoImplementoResultante": "EN_MANTENIMIENTO", "incidencia": { "id": "f607…", "codigo": "INC-000032", "tipo": "DANO" } }
  ],
  "sancion": {
    "fechaInicio": "2026-09-01T20:00:00-05:00",
    "fechaFin": "2026-09-03T20:00:00-05:00",
    "minutosRetraso": 120
  }
}
```

En este ejemplo la devolución llegó 2 horas tarde, sin justificación. Como
`2 × 2 h < 48 h`, se aplica el mínimo de 2 días. `sancion` es `null` cuando no
hay retraso o cuando el retraso está justificado.

`422 Unprocessable Content`, por novedad sin incidencia:

```json
{
  "type": "https://prestamos.itm.edu.co/errores/incidencia-requerida",
  "title": "Incidencia requerida",
  "status": 422,
  "detail": "El implemento PET-014 se recibió con novedad y requiere registrar una incidencia.",
  "instance": "/api/v1/prestamos/7e8f…/devoluciones",
  "codigo": "INCIDENCIA_REQUERIDA",
  "timestamp": "2026-09-01T20:00:00-05:00",
  "errores": [ { "campo": "items[1].incidencia", "mensaje": "Es obligatoria cuando la condición es CON_NOVEDAD." } ]
}
```

`GET /api/v1/devoluciones?alcance=PARCIAL&q=PRE-000148` devuelve una página de
`{ "id", "codigo", "prestamo": { "id", "codigo" }, "usuario", "fechaDevolucion", "alcance", "cantidadImplementos", "conNovedad": boolean }`.

### 10.8 Incidencias

`POST /api/v1/prestamos/{id}/incidencias`

```json
{
  "implementoId": "d4e5f607-1829-4a3b-b5c6-d7e8f90a1b2c",
  "tipo": "PERDIDA",
  "descripcion": "El usuario reporta que el peto se extravió en la cancha 2."
}
```

`201 Created`

```json
{
  "id": "0718293a-4b5c-4d6e-9f70-8192a3b4c5d6",
  "codigo": "INC-000033",
  "tipo": "PERDIDA",
  "estado": "REGISTRADA",
  "descripcion": "El usuario reporta que el peto se extravió en la cancha 2.",
  "fechaRegistro": "2026-09-01T09:30:00-05:00",
  "prestamo": { "id": "7e8f…", "codigo": "PRE-000149", "estado": "DEVOLUCION_PARCIAL" },
  "implemento": { "id": "d4e5…", "codigoInventario": "PET-014", "nombre": "Petos deportivos", "estado": "DADO_DE_BAJA" },
  "usuario": { "id": "1d2e…", "nombres": "Laura", "apellidos": "Gómez" },
  "prestador": { "id": "6f1c…", "nombres": "María", "apellidos": "Rodríguez" }
}
```

`GET /api/v1/incidencias?tipo=DANO` devuelve una página de este mismo objeto.

## 11. Persistencia e integridad

> Implementación vigente del esquema, mapeo de enumeraciones y decisiones de
> persistencia (DM-01 a DM-09): `docs/agent-context/modelo-datos.md`.

- Las migraciones se gestionan con Flyway (`src/main/resources/db/migration`).
  El esquema nunca se genera con `ddl-auto`.
- Restricciones obligatorias:
  - `UNIQUE (usuario.identificacion)` y `UNIQUE (usuario.correo)` (con correo no
    nulo);
  - `UNIQUE (implemento_deportivo.codigo_inventario)` y `UNIQUE (categoria.nombre)`;
  - índice único parcial
    `UNIQUE (detalle_prestamo.implemento_id) WHERE estado = 'PENDIENTE_DEVOLUCION'`,
    que es la garantía de base de datos de RN-06;
  - `CHECK` de los valores de cada enumeración y
    `CHECK (fecha_limite_devolucion > fecha_prestamo)`.
- Índices para RNF-01: `prestamo(usuario_id, estado)`,
  `prestamo(fecha_limite_devolucion) WHERE estado <> 'FINALIZADO'`,
  `implemento_deportivo(categoria_id, estado)`, `devolucion(fecha_devolucion)`,
  `incidencia(fecha_registro)`.
- Bloqueo: el registro de préstamos bloquea las unidades solicitadas con
  `PESSIMISTIC_WRITE`, en orden de UUID para evitar *deadlocks*.
  `ImplementoDeportivo.version` protege la edición administrativa concurrente.
  Un `ObjectOptimisticLockingFailureException` o una violación del índice único
  se traducen a `409 CONFLICTO_CONCURRENCIA`.
- Transacciones: CU-14, CU-16 y CU-18 son `@Transactional` de extremo a extremo
  en el servicio de aplicación. No se hace ninguna escritura fuera de esa
  transacción (RNF-05).
- Datos iniciales:
  - el primer `ADMIN` se crea con una migración o con un *runner* a partir de
    `APP_ADMIN_CORREO` y `APP_ADMIN_CONTRASENA`;
  - las categorías base son las del prototipo: Balones, Raquetas,
    Entrenamiento y Accesorios;
  - los usuarios `USER` se cargan según `PEND-01`.

## 12. Diseño interno esperado (patrones y SOLID)

| Elemento | Responsabilidad | Patrón / principio |
|---|---|---|
| `*Controlador` | Traducir HTTP a DTO y delegar. Sin reglas de negocio. | SRP |
| `RegistrarPrestamoCasoUso` (interfaz) ← `PrestamoServicio` | Entrada única del registro de préstamos. | Facade, DIP |
| `PoliticaElegibilidadPrestamo` + `ValidadorElegibilidad` (`ValidadorUsuarioHabilitado`, `ValidadorLimitePrestamo`, `ValidadorFechas`, `ValidadorDisponibilidad`) | Los validadores son *beans* con `@Order`, inyectados como `List<ValidadorElegibilidad>`. | Chain of Responsibility, OCP |
| `EstadoImplementoBase` y `EstadoDisponible`, `EstadoPrestado`, `EstadoMantenimiento`, `EstadoBaja` | Las transiciones inválidas lanzan `TransicionInvalidaException` por defecto. El estado se persiste como enum y se resuelve con un mapa enum → comportamiento. | State, LSP |
| `PoliticaSancion` (`aplicaA`, `calcular(retraso): Sancion`) con `SancionPorRetrasoSinJustificacion` y `SinSancionPorRetrasoJustificado` | Sin retornos nulos (`Sancion.ninguna()`). Se inyecta `List<PoliticaSancion>`. | Strategy, Null Object, OCP |
| `ActualizadorDisponibilidad`, que implementa `ReservaDisponibilidad`, `LiberacionDisponibilidad` e `IncidenciaDisponibilidad` | Coordina inventario y cierre. Cada servicio depende solo de la interfaz que usa. | Mediator, ISP |
| `PoliticaCierrePrestamo` | Evalúa RN-07. | SRP |
| `java.time.Clock` (*bean*) | Única fuente de "ahora". | DIP, pruebas deterministas |
| `ManejadorGlobalErrores` (`@RestControllerAdvice`) | Traduce excepciones de dominio a Problem Details. | SRP |
| `*Mapper` | Convierte entre entidades y DTO. Las entidades nunca se serializan. | SRP |

Estos elementos incorporan las acciones de prioridad alta y media del análisis
SOLID del Entregable 2.

## 13. Trazabilidad

### 13.1 Requisitos de usuario e historias

| RU / HU | CU | Endpoints |
|---|---|---|
| RU-01 / HU-01 | CU-01, CU-02 | 1, 2 |
| RU-02 / HU-02 | CU-03 | 8 |
| RU-03 / HU-03 | CU-04 | 7, 9 |
| RU-04 / HU-04 | CU-05, CU-06 | 10, 11 |
| RU-05 / HU-05 | CU-07, CU-08 | 4, 5, 6, 13 |
| RU-06 / HU-06 | CU-09, CU-10 | 12, 14, 15 |
| RU-07 / HU-07 | CU-11 | 16 |
| RU-08 / HU-08 | CU-12 | 3, 4 |
| RU-09 / HU-09 | CU-13, CU-14 | 17, 18, 19, 20 |
| RU-10 / HU-10 | CU-15 | 21, 22 |
| RU-11 / HU-11 | CU-13 | 17, 18 |
| RU-12 / HU-12 | CU-14 | 20 |
| RU-13 / HU-13 | CU-16 | 23 |
| RU-14 / HU-14 | CU-17 | 24, 25 |
| RU-15 / HU-15 | CU-16, CU-18, CU-19 | 23, 26, 27, 28 |
| RU-16 / HU-16 | CU-16, CU-18, CU-20 | 23, 26 |

### 13.2 Requisitos funcionales

| RF | Cubierto por |
|---|---|
| RF-01 Autenticación | CU-01; endpoint 1; §5 |
| RF-02 Control de acceso por rol | CU-02; §3.2; §5; endpoint 2 |
| RF-03 Registro de prestadores | CU-03; endpoint 8 |
| RF-04 Consulta de prestadores | CU-04; endpoints 7, 9 |
| RF-05 Búsqueda y filtrado de prestadores | CU-04; endpoint 7 (`q`, `estado`) |
| RF-06 Actualización y desactivación | CU-05, CU-06; endpoints 10, 11 |
| RF-07 Registro de implementos | CU-07, CU-08; endpoints 5, 13 |
| RF-08 Administración del inventario | CU-09, CU-10; endpoints 12, 14, 15 |
| RF-09 Retiro de implementos | CU-11; endpoint 16 |
| RF-10 Consulta de disponibilidad | CU-12; endpoints 3, 4 (públicos) |
| RF-11 Registro de préstamos | CU-14; endpoint 20 |
| RF-12 Validación previa | CU-13, CU-14; endpoints 18, 20; cadena de validadores |
| RF-13 Actualización de inventario por préstamo | CU-14; `ActualizadorDisponibilidad`; índice único parcial |
| RF-14 Consulta de préstamos | CU-13, CU-15; endpoints 18, 21, 22 |
| RF-15 Registro de devolución | CU-16; endpoint 23 |
| RF-16 Estado del implemento devuelto | CU-16 (`condicion` por ítem) |
| RF-17 Registro de incidencias | CU-16 (incidencia en la devolución), CU-18, CU-19; endpoints 23, 26, 27, 28 |
| RF-18 Actualización de inventario por devolución o incidencia | CU-16, CU-18; §6.2 |
| "RF-19" (referencia de HU-14/RU-14) | Ver `D-02`. El comportamiento queda cubierto por CU-17 y los endpoints 24 y 25, trazados a RU-14/HU-14. |

### 13.3 Requisitos no funcionales

| RNF | Cómo se atiende | Verificación |
|---|---|---|
| RNF-01 Rendimiento ≤ 1,5 s con 100 usuarios concurrentes | Paginación obligatoria (máximo 100), índices de §11, proyecciones DTO sin N+1 (`JOIN FETCH` o `@EntityGraph`), sin cargar colecciones completas en los listados. | Prueba de carga (k6 o JMeter) con 100 VU: p95 ≤ 1,5 s en los endpoints de consulta. |
| RNF-02 Disponibilidad 99,5 % mensual | Endpoint `/actuator/health` con verificación de la base de datos, aplicación stateless (se puede escalar horizontalmente), pool de conexiones configurado y migraciones Flyway que no bloquean. La meta se mide en la operación, no en código. | *Health check* en el despliegue (diagrama de despliegue del Entregable 3). |
| RNF-03 Seguridad y control de acceso | Spring Security *deny by default*, `@PreAuthorize` por rol, verificación de cuenta activa y `versionCredenciales` en cada solicitud, BCrypt, secreto JWT fuera del repositorio. | Pruebas de `401`/`403` por cada endpoint protegido. |
| RNF-04 Usabilidad: mensajes comprensibles | `detail` en español claro, `codigo` estable y `errores` por campo. | Pruebas de contrato de errores. |
| RNF-05 Integridad y consistencia | Transacciones de extremo a extremo, bloqueo pesimista, índice único parcial y *rollback* completo cuando falla cualquier validación. | Pruebas de integración con Testcontainers (PostgreSQL), incluida concurrencia. |
| RNF-06 Visualización desde 360 px | Responsabilidad del frontend. La API aporta paginación y respuestas ligeras. | FE-01. |

### 13.4 Reglas de negocio

| RN | CU | Elemento |
|---|---|---|
| RN-01 | CU-13, CU-14 | `ValidadorLimitePrestamo`; `unidadesDisponiblesParaPrestar` |
| RN-02 | CU-14, CU-20 | `ValidadorFechas`, `Prestamo.crear`, tarea de vencimiento |
| RN-03 | CU-13, CU-16 | `PoliticaSancion`; `ValidadorUsuarioHabilitado` (suspensión) |
| RN-04 | CU-08, CU-11, CU-12, CU-14 | Estado del implemento; `ValidadorDisponibilidad` |
| RN-05 | CU-13, CU-14 | `ValidadorUsuarioHabilitado` |
| RN-06 | CU-14, CU-16, CU-18 | `ActualizadorDisponibilidad`, bloqueo, índice único parcial |
| RN-07 | CU-16, CU-18 | `PoliticaCierrePrestamo` |

## 14. Decisiones registradas

| ID | Ambigüedad o tema | Decisión | Fuente del conflicto |
|---|---|---|---|
| `D-01` | En el PDF, RN-01 dice "un usuario podrá tener como máximo cuatro (4)" sin indicar de qué. Una versión de trabajo del documento añade "límite máximo de dos (2) préstamos activos". | El límite es de **4 unidades físicas** en `PENDIENTE_DEVOLUCION` por usuario, sumando todos sus préstamos activos. **No** se limita la cantidad de préstamos. Se descarta el texto de "2 préstamos activos" porque no aparece en el PDF, en el C4 ni en FE-01, que muestra "máximo 4 implementos activos". | PDF §1.4; documento del Entregable 3 |
| `D-02` | HU-14 y la fila RU-14 de la matriz referencian RF-19, pero la tabla de RF del PDF termina en RF-18. | **No se crea un RF-19.** La consulta de devoluciones se conserva como comportamiento obligatorio (CU-17, endpoints 24 y 25), trazado a RU-14 y HU-14, y se apoya en el registro histórico que genera RF-15. La referencia a RF-19 se trata como una inconsistencia editorial del documento fuente. **Acción sugerida:** que el equipo alinee el documento entregable (la versión de trabajo sí redacta un RF-19 que el PDF oficial no incluye). | PDF §1.2, §1.5 y HU-14 |
| `D-03` | La consulta de disponibilidad es para el actor Usuario (RF-10), que no tiene credenciales en el MVP. | Los endpoints 3 y 4 son públicos y de solo lectura, sin datos personales ni identificadores de unidades. | RF-10; FE-01 (catálogo público) |
| `D-04` | El C4 Nivel 4 separa `UsuarioSistema` (Administrador y Prestador) de `UsuarioSolicitante`. | Se usa una sola entidad `Usuario` con un `rol` (`USER`, `PRESTADOR`, `ADMIN`), según lo pedido para el backend. `UsuarioSolicitante` equivale a `Usuario` con rol `USER`. Es una desviación documentada del C4 Nivel 4; el entregable C4 no se modifica. | `docs/modelo-c4/modelo_c4.md` |
| `D-05` | Los prestadores se administran como recurso propio. | `/prestadores` es una fachada administrativa sobre `Usuario` con rol `PRESTADOR`, sin entidad de acceso duplicada. | Issue; RF-03–RF-06 |
| `D-06` | El C4 define `EstadoImplemento` con `DANADO`, `PERDIDO` y `ROBADO`, y el patrón State y FE-01 usan 4 estados. | Se usan 4 estados (§6.2). El motivo (daño, pérdida, robo) queda en la `Incidencia`. Daño u otra novedad lleva a `EN_MANTENIMIENTO`; pérdida o robo, a `DADO_DE_BAJA`. | C4 Nivel 4; ficha State |
| `D-07` | RN-02: ¿quién fija la fecha límite? | La calcula el servidor (`ahora + 24 h`, con `Clock`). El cliente no envía fechas de negocio, por lo que no puede ampliar el plazo. | RN-02; SOLID (DIP del reloj) |
| `D-08` | En la tabla del Entregable 3, la guarda `Vencido → Finalizado` incluye "si hubo retraso, no existe justificación". Así como está, un préstamo con retraso justificado nunca podría cerrarse. | La guarda de cierre es **solo** RN-07. RN-03 se aplica como acción de la devolución tardía y la justificación solo decide la estrategia de sanción. | Entregable 3, §3 |
| `D-09` | `after(24 h)` necesita un disparador temporal. | Se usa una tarea programada idempotente (CU-20) más evaluación al leer. Las reglas nunca dependen de que la tarea ya haya corrido. | Entregable 3 |
| `D-10` | Los estados del préstamo difieren: el C4 usa `ACTIVO/VENCIDO/FINALIZADO` y el Entregable 3 usa `Vigente/Devolución parcial/Vencido/Finalizado`. | Se adoptan los estados del Entregable 3, que es el modelo más reciente. La UI agrupa `VIGENTE` y `DEVOLUCION_PARCIAL` como "Activo". | C4 Nivel 4; Entregable 3 |
| `D-11` | `CondicionDevolucion` en el C4 es `BUENO/CON_DANO/OTRA_NOVEDAD`, y FE-01 usa "Buen estado/Con novedad". | Se usa `BUENO/CON_NOVEDAD`. El detalle de la novedad lo aporta el `tipo` de la incidencia obligatoria. | C4; FE-01 |
| `D-12` | ¿Cómo se registra un daño: con la devolución o aparte? | El daño (`DANO`/`OTRA`) se registra **dentro** de la devolución, de forma atómica. La incidencia independiente (CU-18) solo admite `PERDIDA`/`ROBO`, porque en ese caso la unidad no regresa. | HU-13 esc. 2; HU-15; RNF-05 |
| `D-13` | ¿Cómo obtiene su contraseña un prestador? | El administrador la fija al crearlo (`contrasenaInicial`). No se envían notificaciones porque están fuera del alcance. | RF-03; alcance |
| `D-14` | ¿El administrador opera préstamos? | No. Los endpoints de operación son exclusivos de `PRESTADOR` (RNF-03). Si se requiere, se amplía en otra especificación. | Matriz de actores |
| `D-15` | En el formulario de login de FE-01 la persona elige su rol. | El rol lo determina **solo** el servidor a partir de la cuenta. El selector del frontend es una ayuda visual y no se envía a la API. | FE-01; RNF-03 |

### Pendientes que requieren confirmación del equipo

| ID | Tema | Propuesta provisional |
|---|---|---|
| `PEND-01` | Ningún RF cubre cómo se dan de alta los usuarios `USER` (solicitantes), y la sincronización con sistemas académicos está fuera de alcance. | En el MVP se cargan con una migración o un script de datos semilla. Si el equipo decide que el prestador pueda registrarlos, se necesita una especificación nueva. |
| `PEND-02` | Cambio o recuperación de contraseña. | Fuera del MVP. Mientras tanto, el administrador puede desactivar y volver a crear la cuenta. |
| `PEND-03` | Bloqueo por intentos fallidos de login. | Recomendado. Se especificará si el equipo lo incluye. |
| `PEND-04` | Ciclo de la incidencia (`REGISTRADA → RESUELTA`). FE-01 muestra "Resuelta", pero ningún RF define la resolución. | En el MVP solo se crea `REGISTRADA`. El destino final de la unidad se gestiona con CU-11 (de `EN_MANTENIMIENTO` a `DISPONIBLE` o `DADO_DE_BAJA`). |

## 15. Compatibilidad con el frontend actual (FE-01)

El frontend actual usa datos de demostración en memoria y no consume la API.
Los contratos se validaron contra sus pantallas. Integrarlo requiere estos
ajustes en el frontend, que se especificarán en un issue o spec aparte:

| Pantalla FE-01 | Endpoint | Ajuste necesario en el frontend |
|---|---|---|
| Catálogo público | 3, 4 | Las tarjetas pasan de una por unidad a una por (nombre, categoría), con `unidadesDisponibles`. Actualizar `AvailabilityItem` en `services/api/types.ts` a los nombres de campo en español. |
| Login | 1, 2 | Enviar `identificador`/`contrasena`. El rol se toma de la respuesta y se ignora el selector (`D-15`). Guardar el token en memoria o `sessionStorage` y enviarlo con `Authorization: Bearer`. |
| Prestadores | 7–11 | El formulario de creación necesita el campo `contrasenaInicial` (`D-13`). Separar `nombres` y `apellidos`. "Activar/Desactivar" usa el endpoint 11. |
| Inventario | 12–16 | El formulario de edición no envía `estado`. El selector de estado y el botón "Retirar" usan el endpoint 16. Los conteos de la cabecera salen de `resumen`. |
| Asistente de préstamo, paso 1 | 17, 18 | Mostrar `habilitado`, `motivos`, `unidadesActivas` y `unidadesDisponiblesParaPrestar`. |
| Asistente de préstamo, paso 2 | 19 | Seleccionar unidades por `id`, con un máximo de `unidadesDisponiblesParaPrestar`. |
| Asistente de préstamo, paso 3 | 20 | Las fechas mostradas son una previsualización. Las fechas reales vienen en la respuesta `201`. |
| Préstamos y detalle | 21, 22 | Mostrar `VIGENTE` y `DEVOLUCION_PARCIAL` como "Activo" (`D-10`). Mostrar `codigo` en lugar del ID. |
| Registrar devolución | 23 | Al elegir "Con novedad", capturar en el mismo formulario el tipo y la descripción de la incidencia y enviar todo en una sola solicitud, en lugar de navegar a "Registrar incidencia" (`D-12`). Agregar los campos de retraso justificado cuando el préstamo esté vencido. |
| Devoluciones | 24, 25 | Filtro "Alcance" → `alcance=PARCIAL\|COMPLETA`. |
| Incidencias | 26–28 | El formulario independiente solo ofrece `PERDIDA`/`ROBO` y selecciona préstamo y unidad pendiente. El estado se muestra como "Registrada". |
| Errores | todos | `client.ts` debe leer `codigo` y `detail` del cuerpo `application/problem+json` en lugar del mensaje genérico actual. |

Ningún contrato exige que el frontend aplique reglas de negocio: todas las
validaciones de RN viven en la API y el frontend solo presenta `codigo` y
`detail`.

## 16. Criterios de aceptación de la implementación

Se verifican cuando se implemente el backend contra esta especificación:

1. Todas las rutas usan `/api/v1`. Solo los endpoints 1, 3, 4 y `/actuator/health`
   responden sin token; los demás responden `401` sin token y `403` con un rol no
   autorizado.
2. El login emite un JWT de 60 minutos. Desactivar un prestador invalida sus
   tokens en la siguiente solicitud.
3. Un usuario con 3 unidades activas puede recibir 1 más y recibe `422
   LIMITE_IMPLEMENTOS_EXCEDIDO` si solicita 2.
4. Un usuario con un préstamo vencido o con una sanción activa recibe `422` con
   el código correspondiente, y la consulta de situación lo muestra con
   `habilitado: false`.
5. Prestar una unidad que no está `DISPONIBLE` devuelve `409
   IMPLEMENTO_NO_DISPONIBLE` y no modifica ningún dato.
6. Dos registros concurrentes sobre la misma unidad: exactamente uno tiene
   éxito.
7. `fechaLimiteDevolucion` es siempre `fechaPrestamo + 24 h`, calculada por el
   servidor.
8. Una devolución tardía sin justificación crea una sanción de `max(48 h, 2 ×
   retraso)`. Con justificación no crea sanción.
9. Una devolución `CON_NOVEDAD` sin incidencia devuelve `422
   INCIDENCIA_REQUERIDA`. Con incidencia `DANO`, la unidad queda
   `EN_MANTENIMIENTO`.
10. Una incidencia `PERDIDA`/`ROBO` deja la unidad en `DADO_DE_BAJA`, resuelve
    el detalle y puede finalizar el préstamo.
11. El préstamo pasa a `FINALIZADO` solo cuando no quedan detalles pendientes,
    sin importar si hubo retraso (`D-08`).
12. La tarea de vencimiento marca `VENCIDO` a los préstamos con plazo superado y
    unidades pendientes, y es idempotente.
13. Todos los errores siguen el formato de §4.8 con un `codigo` del catálogo.
14. Los listados respetan la paginación (máximo 100) y los filtros declarados.

## 17. Estrategia de pruebas

| Nivel | Herramienta | Cobertura mínima |
|---|---|---|
| Unitarias de dominio | JUnit 5, AssertJ, `Clock.fixed` | Transiciones de estado del implemento y del préstamo, cada validador de la cadena, cada estrategia de sanción (incluidos los límites de 48 h), `PoliticaCierrePrestamo`. |
| Servicios de aplicación | JUnit 5, Mockito | Orquestación de CU-14, CU-16 y CU-18, *rollback* lógico ante fallos. |
| Integración | `@SpringBootTest`, Testcontainers (PostgreSQL), MockMvc | Cada endpoint: éxito, `400`, `401`, `403`, `404`, `409` y `422` cuando aplique. Restricciones de base de datos y concurrencia (RN-06). |
| Contrato | MockMvc y JSON de ejemplo de §10 | La estructura y los nombres de los campos coinciden con esta especificación. |
| Rendimiento | k6 o JMeter | RNF-01 en los listados y en la disponibilidad. |

Cada prueba referencia en su nombre o con `@DisplayName` el CU, la RN o el
criterio de §16 que verifica.

## 18. Impacto arquitectónico

- `apps/backend/` está inicializado con Spring Boot 4.1, Maven, Flyway y
  configuración por variables de entorno (ver `apps/backend/README.md`).
- Desviaciones respecto al C4 Nivel 4: `D-04`, `D-06`, `D-10` y `D-11`. El
  documento C4 no se modifica porque es un entregable. Las desviaciones quedan
  registradas aquí.
- No se agregan contenedores ni integraciones externas.
