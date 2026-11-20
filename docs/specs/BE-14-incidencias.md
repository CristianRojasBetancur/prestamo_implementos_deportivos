# BE-14 — Incidencias y resolución administrativa

| Campo | Valor |
|---|---|
| Identificador | `BE-14` |
| Relacionado | RU-15, RU-16, RF-17, RF-18, CU-18 y CU-19 de BE-01 |
| Contenedor | API backend |
| Estado | Implementada |

## Alcance

BE-14 registra y consulta incidencias asociadas a una unidad física dentro de un
préstamo. También permite que un administrador cierre una incidencia conservando
la trazabilidad. No crea procesos financieros, cobros, reposiciones ni
integraciones externas.

Las incidencias `DANO` y `OTRA` que acompañan una devolución se registran por
`POST /api/v1/prestamos/{id}/devoluciones` con condición `CON_NOVEDAD` (BE-01
CU-16). El registro independiente de este documento se reserva para `PERDIDA` y
`ROBO`, cuando la unidad no regresó.

## Actores y permisos

| Operación | PRESTADOR | ADMIN |
|---|:-:|:-:|
| Registrar incidencia independiente | Sí | No |
| Consultar listado y detalle | Sí | Sí |
| Resolver incidencia | No | Sí |

## Contrato HTTP

### Registrar

`POST /api/v1/prestamos/{prestamoId}/incidencias`

```json
{
  "implementoId": "c3d4e5f6-0718-4293-a4b5-c6d7e8f90a1b",
  "tipo": "PERDIDA",
  "descripcion": "La unidad no fue localizada."
}
```

La respuesta es `201 Created`, incluye la incidencia creada y `Location`.
`tipo` solo puede ser `PERDIDA` o `ROBO`. La fecha, el prestador y el código son
generados por el servidor.

### Consultar

- `GET /api/v1/incidencias`
- `GET /api/v1/incidencias/{id}`

El listado acepta `q`, `tipo`, `estado`, `prestamoId`, `usuarioId`,
`implementoId`, `desde`, `hasta`, `pagina`, `tamano` y `orden`. `q` busca por
código de incidencia, código de inventario e identidad básica del solicitante.
Las fechas usan el intervalo `[desde, hasta)`.

### Resolver

`PATCH /api/v1/incidencias/{id}/resolucion`

No recibe cuerpo. Responde `200 OK` con la incidencia actualizada y registra
`resueltoPor` y `fechaResolucion`.

Resolver no modifica automáticamente el inventario. Una unidad perdida o robada
permanece `DADO_DE_BAJA`; una unidad con daño u otra novedad permanece en el
estado determinado por su devolución. Cualquier reactivación requiere la
operación administrativa de inventario.

## Reglas de negocio

- El préstamo debe existir y no estar `FINALIZADO`.
- La unidad debe pertenecer al préstamo y su detalle debe estar
  `PENDIENTE_DEVOLUCION`.
- Una unidad ya devuelta o resuelta por incidencia no puede generar otra
  incidencia independiente.
- El prestador autenticado se asocia como registrador; nunca se recibe desde el
  cliente.
- El registro de pérdida/robo y la transición de unidad, detalle, préstamo e
  historial se ejecutan en una única transacción.
- La incidencia inicia en `REGISTRADA`. Resolverla cambia únicamente la
  incidencia a `RESUELTA` y conserva el usuario y el instante de resolución.
- Una incidencia no se elimina como parte de una resolución.

## Errores

Se utiliza BE-01-A y sus códigos existentes: `RECURSO_NO_ENCONTRADO`,
`PRESTAMO_FINALIZADO`, `IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO`,
`UNIDAD_YA_RESUELTA`, `TIPO_INCIDENCIA_NO_PERMITIDO`, `ACCESO_DENEGADO` e
`INCIDENCIA_YA_RESUELTA`.

## Verificación

Las pruebas de `IncidenciaServicioIntegracionTest` cubren tipos independientes,
asociación, estados, transacción, consulta, autorización, auditoría y política
de no reactivación. La matriz de seguridad cubre los tres permisos del módulo.
