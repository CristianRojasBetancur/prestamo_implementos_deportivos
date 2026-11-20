# BE-19 — Alta operativa de solicitantes

| Campo | Valor |
|---|---|
| Identificador | `BE-19` |
| Estado | Implementada |
| Contenedor | API backend |
| Dependencias | BE-01, BE-10, BE-11 |
| Consumidor | Flujo de registro de préstamo de `FE-02` |

## Objetivo

Permitir que un prestador registre en el momento de preparar un préstamo a un
solicitante que todavía no existe, sin crear una cuenta de acceso ni trasladar
reglas de negocio al frontend.

## Alcance

- `POST /api/v1/usuarios`, protegido para `PRESTADOR`.
- Creación de un único usuario con rol `USER` y estado activo.
- Datos requeridos: `identificacion`, `nombres` y `apellidos`.
- No se solicitan correo, contraseña, permisos ni datos académicos.
- La identificación es única; no se permite editar ni eliminar desde este
  flujo.

## Contrato HTTP

### Solicitud

```http
POST /api/v1/usuarios
Content-Type: application/json
Authorization: Bearer <token-de-prestador>
```

```json
{
  "identificacion": "102334455",
  "nombres": "Laura",
  "apellidos": "Gómez"
}
```

Los campos son obligatorios y se recortan en el servicio de aplicación. La
longitud máxima es 40 caracteres para identificación y 120 para cada nombre.

### Respuesta exitosa

`201 Created`, con el mismo DTO de solicitante usado por la búsqueda:

```json
{
  "id": "uuid",
  "nombres": "Laura",
  "apellidos": "Gómez",
  "identificacion": "102334455",
  "activo": true
}
```

El encabezado `Location` identifica `/api/v1/usuarios/{id}`. El recurso de
consulta individual no se agrega por este caso de uso; la URL permite seguir
la convención HTTP sin introducir una vista adicional.

## Autorización y errores

| Caso | Respuesta |
|---|---|
| Token ausente o inválido | `401` `NO_AUTENTICADO` |
| Rol `ADMIN` u otro distinto de `PRESTADOR` | `403` `ACCESO_DENEGADO` |
| Campo faltante, vacío o excede longitud | `400` `VALIDACION_FALLIDA` |
| Identificación ya registrada | `409` `CONFLICTO_UNICIDAD`, con error de campo `identificacion` |

Los mensajes devueltos son aptos para usuario final. No se exponen nombres de
constraints, trazas ni detalles internos de persistencia.

## Reglas de aplicación

1. La operación se ejecuta dentro de una transacción.
2. El servidor asigna `Rol.USER`, `activo = true` y la fecha de creación.
3. La existencia se valida antes de persistir y la restricción única de base de
   datos se traduce también a `409` para cubrir carreras concurrentes.
4. El alta no habilita automáticamente al usuario para un préstamo: el flujo
   de préstamo debe consultar `GET /api/v1/usuarios/{id}/situacion` y dejar que
   BE-10 aplique la elegibilidad autoritativa al confirmar.

## Criterios de aceptación

- Un `PRESTADOR` puede crear un solicitante con los tres datos requeridos.
- Un `ADMIN` recibe `403` y el servicio de registro no se ejecuta.
- La identificación duplicada produce `409` y no crea un segundo usuario.
- La validación de entrada produce `400` sin invocar persistencia.
- El frontend usa el DTO retornado, consulta la situación del nuevo usuario y
  avanza al paso 2 únicamente cuando la API completa el alta.

## Verificación

- `UsuarioControladorWebTest`: autorización, contrato `201`, validación y
  duplicidad de rol.
- `FlujoCompletoApiIntegracionTest`: login de prestador, alta y consulta de
  situación del usuario creado.
- Pruebas de adaptador y del asistente React: payload, `201`, `409` y avance al
  paso 2.
