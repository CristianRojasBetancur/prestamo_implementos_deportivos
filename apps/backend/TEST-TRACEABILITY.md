# Trazabilidad de pruebas del backend

Desde apps/backend/, con Docker disponible:

    mvn clean test

El perfil de pruebas usa ContenedoresPrueba para levantar postgres:17-alpine.
Flyway crea el esquema desde cero y cada clase de integración usa un contenedor
aislado. FabricaFixtures crea datos con sufijos únicos; las credenciales se
generan en tiempo de prueba y usan el dominio reservado pruebas.invalid.
No hay seed, usuario inicial ni contraseña de prueba en src/main o en los
perfiles local/prod.
La fábrica base permite crear USER, ADMIN y PRESTADOR, categorías y unidades
en cualquier EstadoImplemento. Los préstamos, devoluciones, incidencias y
sanciones del escenario E2E se materializan mediante la API para validar el
contrato completo; los tests de aplicación también los construyen directamente
para aislar reglas y transacciones.

## Matriz de escenarios

| Escenario | HU / regla | Evidencia |
|---|---|---|
| Login ADMIN/PRESTADOR, alta/consulta/desactivación de prestador | HU-03, RN-01 | FlujoCompletoApiIntegracionTest.flujoCompleto |
| Categoría, alta de unidad, inventario y disponibilidad pública | HU-07, HU-08, RN-02 | FlujoCompletoApiIntegracionTest.flujoCompleto |
| Situación y elegibilidad del usuario | HU-14, RN-04 | flujoCompleto, erroresCriticos, usuarioVencido |
| Préstamo de varias unidades y disponibilidad exclusiva | HU-15, RN-06 | flujoCompleto, PrestamoServicioIntegracionTest |
| Devolución parcial y cierre final | HU-16, RN-07 | flujoCompleto, DevolucionServicioIntegracionTest |
| Incidencia independiente y resolución administrativa | HU-18, HU-19 | flujoCompleto, IncidenciaServicioIntegracionTest |
| Sanción por retraso | HU-17, RN-03 | flujoCompleto, DevolucionServicioIntegracionTest |
| Credenciales inválidas, rol insuficiente y sesión revocada | RN-01 | erroresCriticos, SeguridadAutenticacionTest |
| Duplicado, unidad no disponible, unidad ajena y operación repetida | RN-02, RN-05 | erroresCriticos, pruebas de incidencia/devolución |
| Fallo sin cambios parciales | RNF-05 | erroresCriticos, DevolucionServicioIntegracionTest.falloDePoliticaRevierteDevolucionYEstadoDeUnidad |
| Concurrencia sobre la misma unidad | RN-06 | PrestamoServicioIntegracionTest |

## Aislamiento y reloj

Los escenarios E2E se regeneran en @BeforeEach; @DirtiesContext evita que un
estado de contexto se comparta con otra clase. RelojPruebas fija el instante
inicial y permite avanzar el tiempo dentro del test para demostrar vencimientos
sin sleep, fechas del equipo ni dependencia del orden de ejecución. La
configuración de JWT usa el mismo Clock, por lo que los tokens son válidos
durante el escenario y su expiración también es determinista.

## Revisión de flakiness

- No se usan sleep, puertos fijos ni una base preexistente.
- El contenedor publica un puerto dinámico y Flyway valida/aplica todas las
  migraciones.
- Los identificadores y correos de fixtures son únicos por escenario.
- La prueba de concurrencia conserva la coordinación transaccional y la
  restricción de disponibilidad de PostgreSQL.
- Si el entorno no tiene Docker, la ejecución debe fallar explícitamente por
  infraestructura; no se debe sustituir por una base local silenciosamente.
