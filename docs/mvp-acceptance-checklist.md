# Checklist de aceptación del MVP

Esta checklist convierte la trazabilidad funcional existente en evidencia de
cierre. `RF-19` no se crea: se conserva la decisión `D-02` de
`BE-01-api-backend-mvp.md`, que traza la consulta de devoluciones a HU-14/RU-14
y CU-17.

| Actor | HU/RU | RF/RN/RNF | Componente | Endpoint o flujo | Prueba/evidencia | Estado |
|---|---|---|---|---|---|---|
| Usuario sistema | HU-01/RU-01 | RF-01, RNF-03 | AutenticacionAutorizacion | `POST /api/v1/auth/login` | `SeguridadAutenticacionTest`, `FlujoCompletoApiIntegracionTest` | Cubierto |
| Administrador/Prestador | HU-01/RU-01 | RF-02, RNF-03 | ConfiguracionSeguridad | Rutas protegidas | `SeguridadAutenticacionTest.matriz` | Cubierto |
| Administrador | HU-02/HU-03/HU-04 | RF-03 a RF-06 | GestionPrestadores | `/api/v1/providers` | `PrestadorControladorWebTest`, flujo completo | Cubierto |
| Administrador | HU-05 a HU-08 | RF-07 a RF-09, RN-04 | GestionInventario | `/api/v1/categorias`, `/api/v1/inventory/units` | `InventarioControladorWebTest`, `InventarioServicioTest` | Cubierto |
| Usuario | HU-08 | RF-10, RNF-01 | CatalogoDisponibilidad | `GET /api/v1/disponibilidad` | `DisponibilidadControladorWebTest`, `availability.js` | Cubierto |
| Prestador | HU-09/HU-10 | RF-11, RF-12, RN-01, RN-02, RN-04, RN-05 | GestionPrestamos + CadenaElegibilidad | `/api/v1/usuarios/{id}/situacion`, `POST /api/v1/prestamos` | `ValidadoresElegibilidadTest`, `PrestamoServicioIntegracionTest` | Cubierto |
| Prestador | HU-11/HU-12 | RF-13, RF-14, RN-06 | ActualizacionDisponibilidad | `/api/v1/prestamos` | `FlujoCompletoApiIntegracionTest`, prueba de concurrencia | Cubierto |
| Prestador | HU-13/HU-14 | RF-15, RF-16, D-02 | GestionDevoluciones | `/api/v1/prestamos/{id}/devoluciones`, `/api/v1/devoluciones` | `DevolucionServicioIntegracionTest`, `PrestamoConsultaIntegracionTest` | Cubierto |
| Prestador | HU-13/HU-15 | RF-17, RF-18, RN-07 | GestionIncidencias + Mediator | `/api/v1/prestamos/{id}/incidencias` | `IncidenciaServicioIntegracionTest`, flujo completo | Cubierto |
| Prestador | HU-13/HU-16 | RN-03 | Strategy de sanciones | Devolución tardía | `PoliticasSancionTest`, `DevolucionServicioIntegracionTest` | Cubierto |
| Administrador/Prestador | Todas | RNF-04 | ManejadorGlobalErrores | Problem Details | `ContratoErroresMvcTest`, `MapeoErroresTest` | Cubierto |
| Sistema | Todas | RNF-05 | Servicios transaccionales + PostgreSQL | Préstamo, devolución, incidencia | integración Testcontainers y rollback | Cubierto |
| Sistema | Todas | RNF-01 | Persistencia y paginación | Consultas críticas | k6 + `performance/README.md` | Evidencia local requerida |
| Sistema | Todas | RNF-02 | Actuator + Compose | health/readiness | `ArranqueAplicacionTest`, smoke test | Disponibilidad mensual no demostrable localmente |
| Sistema | Todas | Seguridad | Docker runtime | Imagen no-root | CI: `id -u`, Dockerfile, `.dockerignore` | Cubierto por CI |
| Sistema | Todas | Alcance | Arquitectura | Exclusiones del MVP | `contexto-proyecto.md`, README raíz | Cubierto |

## Reglas RN-01…RN-07

- RN-01: máximo cuatro unidades activas — `ValidadoresElegibilidadTest.limiteCuatroUnidades`.
- RN-02: plazo máximo de 24 horas — `ValidadoresElegibilidadTest.fechasSeLimitanA24Horas`.
- RN-03: sanción `max(48 h, 2 × retraso)` — `PoliticasSancionTest` y devolución de integración.
- RN-04: unidades no prestables — `InventarioServicioTest` y validadores de disponibilidad.
- RN-05: préstamos vencidos bloquean — `ValidadoresElegibilidadTest.vencidoQuedaBloqueado`.
- RN-06: exclusividad concurrente — `PrestamoServicioIntegracionTest`.
- RN-07: cierre sin detalles pendientes — `DevolucionServicioIntegracionTest` y flujo completo.

## Diferimientos y exclusiones

- RNF-01 requiere conservar resultados de k6, dataset, JVM, pool y planes SQL;
  la ejecución no se presenta como evidencia permanente si no se adjuntan esos resultados.
- RNF-02 requiere infraestructura operacional para demostrar 99,5 % mensual;
  localmente solo se verifican health/readiness y capacidad stateless.
- ERP, mapas, geolocalización, notificaciones externas, pagos, reservas
  independientes y analítica avanzada permanecen explícitamente fuera del MVP.
