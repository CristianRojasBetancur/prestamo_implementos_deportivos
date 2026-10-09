# Plantilla de análisis y selección de patrones
# GoF

Instrumento de apoyo para justificar patrones GoF no repetidos dentro del alcance de un producto mínimo viable.

Uso sugerido: llenar primero el alcance del MVP, luego analizar necesidades de diseño reales y finalmente aprobar únicamente los patrones que aporten valor y no generen sobreingeniería.

## 1. Datos generales del proyecto

**Nombre del proyecto:** Sistema de Préstamo de Implementos Deportivos  
**Equipo de trabajo:** Cristian Rojas Betancur; Diego Alexander Ardila Sanchez; Mariana Duque Grajales; Santiago Cortes Zuluaga  
**Asignatura:** [Por diligenciar]  
**Docente:** [Por diligenciar]  
**Fecha:** [Por diligenciar]

## 2. Descripción general del proyecto

**Problema que resuelve el proyecto:**

El proceso de préstamo se realiza principalmente de forma manual. Esto dificulta consultar la disponibilidad, validar la situación del usuario, controlar las fechas de devolución, mantener sincronizado el inventario y conservar trazabilidad sobre devoluciones, daños, pérdidas o robos.

**Objetivo general del sistema:**

Automatizar y dar trazabilidad al ciclo de vida del préstamo de implementos deportivos, desde la autenticación y consulta de disponibilidad hasta el registro de préstamos, devoluciones e incidencias, aplicando las reglas de negocio del MVP.

**Descripción del producto propuesto:**

Plataforma web compuesta por una aplicación React/Vite, una API Java/Spring Boot y una base de datos PostgreSQL. La aplicación permite que los usuarios consulten disponibilidad, que los administradores gestionen prestadores e inventario y que los prestadores registren préstamos, devoluciones e incidencias.

## 3. Alcance del MVP

### 3.1 Funcionalidades incluidas en el MVP

| ID | Funcionalidad del MVP | Prioridad | Justificación |
|---|---|---|---|
| F1 | Consultar disponibilidad | Alta | Permite al usuario consultar los implementos deportivos disponibles y filtrarlos por categoría antes de solicitar un préstamo. |
| F2 | Gestionar préstamos | Alta | Permite al prestador consultar la situación del usuario, validar su elegibilidad y registrar la entrega de uno o varios implementos. |
| F3 | Gestionar devoluciones | Alta | Permite registrar devoluciones totales o parciales, almacenar las fechas y actualizar el estado de los implementos recibidos. |
| F4 | Gestionar incidencias | Alta | Permite registrar daños, pérdidas, robos u otras novedades asociadas a los implementos y préstamos. |
| F5 | Gestionar prestadores | Alta | Permite al administrador registrar, consultar, actualizar y desactivar prestadores habilitados para operar el sistema. |
| F6 | Gestionar inventario | Alta | Permite al administrador registrar, consultar, actualizar y retirar implementos, categorías, unidades físicas y estados del inventario. |

Estas funcionalidades agrupan las historias HU-01 a HU-16 y los requisitos RF-01 a RF-19 descritos en el contexto del proyecto.

### 3.2 Restricciones del MVP

- El proyecto debe implementar **5 patrones GoF**.
- Los patrones no se pueden repetir.
- Cada patrón debe responder a una necesidad real del sistema.
- La selección debe evitar sobreingeniería.
- Los patrones deben ser viables dentro del tiempo y alcance del MVP.
- El Nivel 4 del modelo C4 es una propuesta técnica; las clases auxiliares que no aparecen allí se incorporan únicamente como extensiones mínimas para materializar los patrones.

### 3.3 Funcionalidades fuera del MVP

- Integración con ERP u otros sistemas institucionales.
- Mapas, geolocalización o ubicación automática de implementos.
- Notificaciones externas por correo, SMS, mensajería o servicios de terceros.
- Reservas anticipadas independientes del proceso de préstamo.
- Pagos, cobros o gestión financiera de pérdidas.
- Analítica avanzada, recomendaciones o reportes complejos.
- Sincronización con otros inventarios o sistemas académicos.

## 4. Matriz global de análisis de patrones del proyecto

Esta matriz se diligencia antes de aprobar patrones, para identificar necesidades reales de diseño a nivel de proyecto.

| Necesidad de diseño detectada | Módulo o funcionalidad afectada | Tipo de problema | Patrón candidato | ¿Aporta valor al MVP? | ¿Se solapa con otro patrón? |
|---|---|---|---|---|---|
| El comportamiento de una unidad física cambia según su estado y no todas las acciones son válidas en todos los estados. | `ImplementoDeportivo`, `GestionInventario`, `ActualizacionDisponibilidad` | Comportamiento variable por estado | State | Sí | No. State administra transiciones; Mediator coordina componentes. |
| La elegibilidad de un préstamo depende de varias reglas independientes que pueden crecer o cambiar. | `PoliticaElegibilidadPrestamo`, `GestionPrestamos` | Validaciones encadenadas y alto acoplamiento | Chain of Responsibility | Sí | No. Chain organiza validaciones; Strategy representa políticas alternativas. |
| La sanción aplicable depende de la situación del retraso, especialmente de la existencia de justificación. | `PoliticaSancion`, `PrestamoServicio` | Multiplicidad de algoritmos/políticas | Strategy | Sí | No. Strategy encapsula el cálculo de sanción y no valida la elegibilidad completa. |
| El controlador no debería conocer todos los repositorios, validadores y servicios que intervienen al registrar un préstamo. | `PrestamoControlador`, `PrestamoServicio` | Alto acoplamiento entre cliente y subsistemas | Facade | Sí | No. Facade simplifica el acceso externo al caso de uso. |
| Préstamos, devoluciones e incidencias deben actualizar el inventario mediante una coordinación común y consistente. | `ActualizadorDisponibilidad`, `GestionPrestamos`, `GestionDevoluciones`, `GestionIncidencias` | Dependencias cruzadas y coordinación distribuida | Mediator | Sí | No. Mediator coordina colegas internos; Facade atiende al cliente del caso de uso. |

## 5.1 Ficha de análisis del patrón: State

### 5.1 Identificación general

**Nombre del patrón:** State  
**Categoría:** [ ] Creacional [ ] Estructural [x] De comportamiento  
**Módulo, componente o funcionalidad donde se aplicará:** `ImplementoDeportivo`, `GestionInventario` y `ActualizacionDisponibilidad`.

### 5.2 Problema de diseño que se quiere resolver

Una unidad física puede estar disponible, prestada, en mantenimiento o dada de baja. Las operaciones permitidas cambian según ese estado: una unidad en mantenimiento o dada de baja no puede prestarse, una unidad prestada no puede asignarse simultáneamente a otro préstamo y una devolución puede dejarla disponible o fuera de servicio. Si todas estas condiciones se implementan mediante condicionales dispersos, la lógica de transición queda acoplada a los servicios que realizan las operaciones.

### 5.3 Evidencias del problema en el proyecto

- [x] Alto acoplamiento
- [ ] Multiplicidad de algoritmos
- [x] Cambio frecuente de comportamiento
- [ ] Complejidad en la creación de objetos
- [ ] Necesidad de desacoplar emisor y receptor
- [x] Necesidad de ampliar funcionalidad sin modificar la clase base
- [ ] Interfaz incompatible
- [x] Otro: restricciones de transición entre estados del inventario.

**Explicación contextualizada en el proyecto:**

El Nivel 4 ya define `EstadoImplemento`, `estaDisponible()`, `puedePrestarse()` y `cambiarEstado()`. Esto evidencia que el comportamiento depende del estado actual. Las reglas RN-04 y RN-06 hacen que una transición incorrecta pueda permitir un préstamo inválido o dos préstamos activos para la misma unidad.

### 5.4 Relación con el MVP

- **¿Por qué este problema sí debe resolverse en el MVP y no en una versión futura?** Porque la disponibilidad y la exclusividad de una unidad son parte central del registro de préstamos, devoluciones e incidencias.
- **¿Qué riesgo habría si no se aplica este patrón en el MVP?** Se podrían prestar implementos en mantenimiento o dados de baja, duplicar préstamos activos o reactivar incorrectamente una unidad con incidencia.
- **¿La implementación del patrón es razonable para el tiempo y complejidad del MVP?** [x] Sí [ ] No

**Justificación:**

La implementación puede limitarse a una interfaz de estado y a los estados necesarios para el MVP. No requiere introducir un motor de reglas ni alterar los contenedores definidos en C4.

### 5.5 Análisis de adecuación del patrón

- **¿Qué parte del sistema cambia o podría cambiar?** Las acciones válidas y las transiciones al prestar, devolver, reportar una incidencia, enviar a mantenimiento o dar de baja una unidad.
- **¿Qué se desea desacoplar?** La entidad `ImplementoDeportivo` y los servicios de operación respecto de las condiciones específicas de cada estado.
- **¿Qué ventaja concreta ofrece este patrón en este proyecto?** Centraliza las transiciones válidas y evita repetir condicionales en préstamos, devoluciones e incidencias.
- **¿Qué principio de diseño ayuda a cumplir?** [x] Abierto/Cerrado [x] Responsabilidad Única [ ] Inversión de Dependencias [ ] Segregación de Interfaces [ ] Otro: —

**Justificación técnica:**

Cada estado conoce las operaciones que permite y las transiciones que puede realizar. Agregar un estado futuro no obliga a modificar todos los servicios que usan el implemento.

### 5.6 Alternativas consideradas

| Patrón alternativo | ¿Por qué podría confundirse? | ¿Por qué no es el mejor en este caso? |
|---|---|---|
| Strategy | También encapsula comportamientos variables. | Strategy selecciona algoritmos intercambiables; aquí el comportamiento está determinado por el estado vigente y sus transiciones. |
| Chain of Responsibility | Puede encadenar validaciones antes de una operación. | La cadena valida una solicitud, pero no representa el ciclo de vida persistente de la unidad física. |

### 5.7 No repetición ni solapamiento

- **¿Este patrón ya fue usado en otra parte del proyecto?** [ ] Sí [x] No
- **¿Su propósito funcional se solapa con otro patrón seleccionado?** [ ] Sí [x] No
- **Explicación:** State administra el comportamiento interno de una unidad según su estado; Mediator solo coordina los componentes que solicitan actualizaciones.

### 5.8 Aplicación concreta al diseño

| Rol del patrón | Clase/componente del proyecto |
|---|---|
| Contexto | `ImplementoDeportivo` |
| Estado | `EstadoImplemento` o interfaz `EstadoImplementoBehavior` |
| Estados concretos | `EstadoDisponible`, `EstadoPrestado`, `EstadoMantenimiento`, `EstadoBaja` |
| Cliente de la transición | `ActualizadorDisponibilidad` |

**Descripción breve de cómo funcionará en el sistema:**

`ImplementoDeportivo` delegará en su estado actual la validación de acciones como prestar, liberar, enviar a mantenimiento o dar de baja. `ActualizadorDisponibilidad` solicitará las transiciones dentro de la misma operación transaccional de préstamo, devolución o incidencia.

**Fragmento de pseudocódigo, diagrama o referencia UML:**

```text
interface EstadoImplementoBehavior {
    puedePrestarse(): boolean
    prestar(): EstadoImplementoBehavior
    recibirDevolucion(condicion): EstadoImplementoBehavior
    registrarIncidencia(tipo): EstadoImplementoBehavior
}

class ImplementoDeportivo {
    estado: EstadoImplementoBehavior

    puedePrestarse() = estado.puedePrestarse()
    cambiarEstado(nuevoEstado) = estado = nuevoEstado
}
```

### 5.9 Conclusión de decisión

- **Decisión final sobre este patrón:** [x] Se aprueba [ ] Se descarta
- **Justificación final:** El patrón resuelve directamente la variación de comportamiento definida por los estados del inventario y protege reglas críticas del préstamo sin introducir funcionalidades fuera del MVP.

## 5.1 Ficha de análisis del patrón: Chain of Responsibility

### 5.1 Identificación general

**Nombre del patrón:** Chain of Responsibility  
**Categoría:** [ ] Creacional [ ] Estructural [x] De comportamiento  
**Módulo, componente o funcionalidad donde se aplicará:** `PoliticaElegibilidadPrestamo` y `GestionPrestamos`.

### 5.2 Problema de diseño que se quiere resolver

Registrar un préstamo exige ejecutar varias validaciones: usuario habilitado, ausencia de préstamos vencidos, límite máximo de cuatro implementos, duración máxima, estado prestable de las unidades y exclusividad de cada unidad. Si `PrestamoServicio` contiene todas las validaciones en un único método, cada nueva regla aumenta su complejidad y obliga a modificar una clase central.

### 5.3 Evidencias del problema en el proyecto

- [x] Alto acoplamiento
- [ ] Multiplicidad de algoritmos
- [x] Cambio frecuente de comportamiento
- [ ] Complejidad en la creación de objetos
- [ ] Necesidad de desacoplar emisor y receptor
- [x] Necesidad de ampliar funcionalidad sin modificar la clase base
- [ ] Interfaz incompatible
- [x] Otro: conjunto ordenado de reglas de elegibilidad.

**Explicación contextualizada en el proyecto:**

El Nivel 4 concentra RN-01, RN-02, RN-04 y RN-05 en `PoliticaElegibilidadPrestamo`, mientras que el flujo también debe verificar la exclusividad de la unidad. La cantidad de reglas y su trazabilidad en RF-12 justifican separar validadores pequeños.

### 5.4 Relación con el MVP

- **¿Por qué este problema sí debe resolverse en el MVP y no en una versión futura?** Porque RF-12 exige validar antes de registrar y esas validaciones determinan si el préstamo puede crear cambios en el inventario.
- **¿Qué riesgo habría si no se aplica este patrón en el MVP?** Una regla podría omitirse, ejecutarse en un orden incorrecto o quedar duplicada en varios servicios, provocando préstamos inválidos e inconsistencias.
- **¿La implementación del patrón es razonable para el tiempo y complejidad del MVP?** [x] Sí [ ] No

**Justificación:**

Cada manejador puede ser una clase pequeña con una única validación. La cadena se construye una vez en `PoliticaElegibilidadPrestamo` y no requiere infraestructura adicional.

### 5.5 Análisis de adecuación del patrón

- **¿Qué parte del sistema cambia o podría cambiar?** Las reglas de elegibilidad y el orden en que se ejecutan antes de crear un préstamo.
- **¿Qué se desea desacoplar?** `PrestamoServicio` respecto de los detalles de cada validación.
- **¿Qué ventaja concreta ofrece este patrón en este proyecto?** Permite agregar, retirar o reordenar reglas sin convertir el servicio de préstamo en una clase monolítica.
- **¿Qué principio de diseño ayuda a cumplir?** [x] Abierto/Cerrado [x] Responsabilidad Única [ ] Inversión de Dependencias [ ] Segregación de Interfaces [ ] Otro: —

**Justificación técnica:**

Cada manejador implementa una misma interfaz y responde por una regla concreta. La política coordina la cadena, pero no necesita conocer la implementación interna de cada validación.

### 5.6 Alternativas consideradas

| Patrón alternativo | ¿Por qué podría confundirse? | ¿Por qué no es el mejor en este caso? |
|---|---|---|
| Strategy | Las reglas son comportamientos intercambiables. | Strategy seleccionaría una política completa; aquí se deben ejecutar varias validaciones consecutivas en una misma solicitud. |
| Template Method | Podría definir un algoritmo fijo de validación. | El conjunto de reglas puede crecer o cambiar de orden; una jerarquía fija sería menos flexible que una cadena configurable. |

### 5.7 No repetición ni solapamiento

- **¿Este patrón ya fue usado en otra parte del proyecto?** [ ] Sí [x] No
- **¿Su propósito funcional se solapa con otro patrón seleccionado?** [ ] Sí [x] No
- **Explicación:** Chain valida la elegibilidad de una solicitud; State determina las acciones válidas de una unidad y Strategy selecciona una política de sanción.

### 5.8 Aplicación concreta al diseño

| Rol del patrón | Clase/componente del proyecto |
|---|---|
| Cliente | `PoliticaElegibilidadPrestamo` |
| Manejador abstracto | `ValidadorElegibilidad` |
| Manejadores concretos | `ValidadorUsuarioHabilitado`, `ValidadorLimitePrestamo`, `ValidadorFechas`, `ValidadorDisponibilidad` |
| Solicitud | `SolicitudElegibilidadPrestamo` o `CrearPrestamoSolicitud` |

**Descripción breve de cómo funcionará en el sistema:**

La política construirá una cadena con las validaciones necesarias. Cada manejador detendrá el flujo con un error de negocio si su regla falla; si la validación es correcta, delegará al siguiente manejador. Solo después de completar la cadena se creará el préstamo.

**Fragmento de pseudocódigo, diagrama o referencia UML:**

```text
validadorUsuario
    .siguiente(validadorLimite)
    .siguiente(validadorFechas)
    .siguiente(validadorDisponibilidad)

validadorUsuario.validar(solicitud)
```

### 5.9 Conclusión de decisión

- **Decisión final sobre este patrón:** [x] Se aprueba [ ] Se descarta
- **Justificación final:** Las reglas de elegibilidad son numerosas, críticas y explícitas en el contexto. Separarlas reduce complejidad y permite mantener trazabilidad con las RN del MVP.

## 5.1 Ficha de análisis del patrón: Strategy

### 5.1 Identificación general

**Nombre del patrón:** Strategy  
**Categoría:** [ ] Creacional [ ] Estructural [x] De comportamiento  
**Módulo, componente o funcionalidad donde se aplicará:** `GestionPrestamos`, `PrestamoServicio` y la política de sanciones derivada de devoluciones tardías.

### 5.2 Problema de diseño que se quiere resolver

El resultado de una devolución tardía no siempre es el mismo: RN-03 establece una suspensión cuando el retraso no tiene justificación, mientras que una devolución justificada requiere un tratamiento diferente. Si el servicio de préstamos contiene todos los cálculos y excepciones de sanción, cambiar la política institucional obligaría a modificar el flujo principal del préstamo.

### 5.3 Evidencias del problema en el proyecto

- [ ] Alto acoplamiento
- [x] Multiplicidad de algoritmos
- [x] Cambio frecuente de comportamiento
- [ ] Complejidad en la creación de objetos
- [ ] Necesidad de desacoplar emisor y receptor
- [x] Necesidad de ampliar funcionalidad sin modificar la clase base
- [ ] Interfaz incompatible
- [x] Otro: políticas distintas según justificación y duración del retraso.

**Explicación contextualizada en el proyecto:**

El contexto define una regla específica de sanción: el retraso sin justificación genera una suspensión equivalente al doble del retraso, con mínimo de dos días. La existencia explícita de una condición de justificación representa una variación de política que puede encapsularse sin mezclarla con el registro de la devolución.

### 5.4 Relación con el MVP

- **¿Por qué este problema sí debe resolverse en el MVP y no en una versión futura?** Porque la sanción por retraso forma parte de las reglas de negocio y del historial del usuario, por lo que debe resolverse al registrar una devolución.
- **¿Qué riesgo habría si no se aplica este patrón en el MVP?** La política podría quedar duplicada en devoluciones y consultas, o aplicarse de forma distinta cuando se agregue una justificación o una política institucional diferente.
- **¿La implementación del patrón es razonable para el tiempo y complejidad del MVP?** [x] Sí [ ] No

**Justificación:**

La extensión mínima consiste en una interfaz de cálculo y dos implementaciones concretas. El flujo de devolución continúa siendo el mismo y solo delega el cálculo de la sanción.

### 5.5 Análisis de adecuación del patrón

- **¿Qué parte del sistema cambia o podría cambiar?** El cálculo de la suspensión según duración del retraso, existencia de justificación y futuras políticas institucionales.
- **¿Qué se desea desacoplar?** `PrestamoServicio` y `DevolucionServicio` respecto del algoritmo de sanción.
- **¿Qué ventaja concreta ofrece este patrón en este proyecto?** Permite modificar la política de sanción sin alterar la coordinación de la devolución ni el modelo de `Prestamo`.
- **¿Qué principio de diseño ayuda a cumplir?** [x] Abierto/Cerrado [x] Responsabilidad Única [x] Inversión de Dependencias [ ] Segregación de Interfaces [ ] Otro: —

**Justificación técnica:**

El servicio depende de una abstracción `PoliticaSancion`, no de una fórmula concreta. Cada estrategia contiene una sola forma de determinar la sanción.

### 5.6 Alternativas consideradas

| Patrón alternativo | ¿Por qué podría confundirse? | ¿Por qué no es el mejor en este caso? |
|---|---|---|
| State | La sanción puede modificar el estado del usuario. | La sanción es un cálculo/política aplicada durante una operación; no representa el ciclo de vida completo del usuario. |
| Chain of Responsibility | Podría encadenar verificaciones de retraso y justificación. | Chain organiza pasos de validación; aquí se necesita seleccionar un algoritmo completo de cálculo. |

### 5.7 No repetición ni solapamiento

- **¿Este patrón ya fue usado en otra parte del proyecto?** [ ] Sí [x] No
- **¿Su propósito funcional se solapa con otro patrón seleccionado?** [ ] Sí [x] No
- **Explicación:** Strategy calcula la sanción; Chain decide si la solicitud de préstamo es elegible. Son momentos y responsabilidades diferentes.

### 5.8 Aplicación concreta al diseño

| Rol del patrón | Clase/componente del proyecto |
|---|---|
| Contexto | `PrestamoServicio` o `DevolucionServicio` |
| Estrategia | `PoliticaSancion` |
| Estrategia concreta 1 | `SancionPorRetrasoSinJustificacion` |
| Estrategia concreta 2 | `SinSancionPorRetrasoJustificado` |

**Descripción breve de cómo funcionará en el sistema:**

Al procesar una devolución tardía, el servicio seleccionará la estrategia según la justificación registrada. La estrategia sin justificación calculará el doble del retraso con mínimo de dos días; la estrategia justificada no generará la suspensión correspondiente a RN-03.

**Fragmento de pseudocódigo, diagrama o referencia UML:**

```text
interface PoliticaSancion {
    calcular(retraso, justificada): Sancion?
}

politica = justificada
    ? SinSancionPorRetrasoJustificado()
    : SancionPorRetrasoSinJustificacion()

sancion = politica.calcular(diasRetraso, justificada)
```

### 5.9 Conclusión de decisión

- **Decisión final sobre este patrón:** [x] Se aprueba [ ] Se descarta
- **Justificación final:** La variación del resultado por justificación está definida por la regla RN-03. Strategy la encapsula con una extensión pequeña y evita contaminar los servicios de préstamo y devolución.

## 5.1 Ficha de análisis del patrón: Facade

### 5.1 Identificación general

**Nombre del patrón:** Facade  
**Categoría:** [ ] Creacional [x] Estructural [ ] De comportamiento  
**Módulo, componente o funcionalidad donde se aplicará:** `PrestamoServicio` como entrada simplificada al flujo de registro de préstamos.

### 5.2 Problema de diseño que se quiere resolver

El registro de un préstamo requiere consultar al usuario, validar reglas de elegibilidad, obtener unidades, verificar sus estados, actualizar disponibilidad, guardar el préstamo y construir una respuesta. Si `PrestamoControlador` conoce directamente todos esos elementos, queda acoplado a la estructura interna de la API y se dificulta cambiar la coordinación del caso de uso.

### 5.3 Evidencias del problema en el proyecto

- [x] Alto acoplamiento
- [ ] Multiplicidad de algoritmos
- [ ] Cambio frecuente de comportamiento
- [ ] Complejidad en la creación de objetos
- [x] Necesidad de desacoplar emisor y receptor
- [ ] Necesidad de ampliar funcionalidad sin modificar la clase base
- [ ] Interfaz incompatible
- [x] Otro: subsistema compuesto por validadores, repositorios y servicios.

**Explicación contextualizada en el proyecto:**

El diagrama del flujo de préstamo muestra `PrestamoControlador`, `PrestamoServicio`, `PoliticaElegibilidadPrestamo`, `ActualizadorDisponibilidad`, `PrestamoRepositorio` e `ImplementoDeportivoRepositorio`. Esa colaboración debe quedar detrás de una interfaz de aplicación estable.

### 5.4 Relación con el MVP

- **¿Por qué este problema sí debe resolverse en el MVP y no en una versión futura?** Porque el registro de préstamos es una funcionalidad principal y el controlador debe exponer una operación HTTP sencilla y estable.
- **¿Qué riesgo habría si no se aplica este patrón en el MVP?** El controlador podría duplicar reglas, conocer detalles de persistencia y producir diferentes secuencias de actualización en cada endpoint.
- **¿La implementación del patrón es razonable para el tiempo y complejidad del MVP?** [x] Sí [ ] No

**Justificación:**

El modelo C4 ya contiene `PrestamoServicio`; el patrón formaliza su responsabilidad sin requerir un nuevo contenedor ni una jerarquía compleja.

### 5.5 Análisis de adecuación del patrón

- **¿Qué parte del sistema cambia o podría cambiar?** La coordinación interna del registro y consulta de préstamos.
- **¿Qué se desea desacoplar?** `PrestamoControlador` respecto de repositorios, validadores y actualización de disponibilidad.
- **¿Qué ventaja concreta ofrece este patrón en este proyecto?** Proporciona una entrada única para el caso de uso, facilita las pruebas del flujo y protege la separación entre API y dominio.
- **¿Qué principio de diseño ayuda a cumplir?** [ ] Abierto/Cerrado [x] Responsabilidad Única [x] Inversión de Dependencias [ ] Segregación de Interfaces [ ] Otro: —

**Justificación técnica:**

El controlador traduce HTTP a una solicitud y recibe una respuesta. `PrestamoServicio` coordina el caso de uso y oculta los detalles del subsistema.

### 5.6 Alternativas consideradas

| Patrón alternativo | ¿Por qué podría confundirse? | ¿Por qué no es el mejor en este caso? |
|---|---|---|
| Mediator | También centraliza colaboraciones entre objetos. | Mediator coordina colegas internos; Facade ofrece una interfaz simplificada al controlador, que es el cliente externo del subsistema. |
| Adapter | Puede envolver una interfaz compleja detrás de otra. | No se está corrigiendo una incompatibilidad de interfaces; se está simplificando el acceso a un flujo de negocio. |

### 5.7 No repetición ni solapamiento

- **¿Este patrón ya fue usado en otra parte del proyecto?** [ ] Sí [x] No
- **¿Su propósito funcional se solapa con otro patrón seleccionado?** [ ] Sí [x] No
- **Explicación:** Facade simplifica la relación controlador-servicio; Mediator coordina las relaciones internas entre operaciones que modifican disponibilidad.

### 5.8 Aplicación concreta al diseño

| Rol del patrón | Clase/componente del proyecto |
|---|---|
| Facade | `PrestamoServicio` |
| Cliente | `PrestamoControlador` |
| Subsistema de validación | `PoliticaElegibilidadPrestamo` |
| Subsistemas de inventario y persistencia | `ActualizadorDisponibilidad`, `PrestamoRepositorio`, `ImplementoDeportivoRepositorio` |

**Descripción breve de cómo funcionará en el sistema:**

El controlador invocará `crearPrestamo(CrearPrestamoSolicitud)` en `PrestamoServicio`. El servicio validará la solicitud, cargará las unidades, pedirá al mediador la actualización de disponibilidad, persistirá el préstamo y devolverá `PrestamoRespuesta`.

**Fragmento de pseudocódigo, diagrama o referencia UML:**

```text
PrestamoControlador.crear(solicitud) {
    return prestamoServicio.crearPrestamo(solicitud)
}

PrestamoServicio.crearPrestamo(solicitud) {
    elegibilidad.validar(solicitud)
    unidades = inventario.buscarDisponibles(solicitud.unidades)
    disponibilidad.reservar(unidades)
    prestamo = repositorio.guardar(Prestamo.crear(solicitud, unidades))
    return mapper.aRespuesta(prestamo)
}
```

### 5.9 Conclusión de decisión

- **Decisión final sobre este patrón:** [x] Se aprueba [ ] Se descarta
- **Justificación final:** `PrestamoServicio` ya es el punto natural de coordinación del flujo. Formalizarlo como Facade reduce el acoplamiento del controlador sin agregar una responsabilidad ajena al MVP.

## 5.1 Ficha de análisis del patrón: Mediator

### 5.1 Identificación general

**Nombre del patrón:** Mediator  
**Categoría:** [ ] Creacional [ ] Estructural [x] De comportamiento  
**Módulo, componente o funcionalidad donde se aplicará:** `ActualizacionDisponibilidad`, conectado con préstamos, devoluciones e incidencias.

### 5.2 Problema de diseño que se quiere resolver

El estado de una unidad y el cierre de un préstamo pueden cambiar desde el registro del préstamo, la devolución o una incidencia. Si cada componente actualiza directamente inventario, préstamo y persistencia, se crean dependencias cruzadas y es posible que cada flujo aplique una secuencia diferente.

### 5.3 Evidencias del problema en el proyecto

- [x] Alto acoplamiento
- [ ] Multiplicidad de algoritmos
- [x] Cambio frecuente de comportamiento
- [ ] Complejidad en la creación de objetos
- [x] Necesidad de desacoplar emisor y receptor
- [ ] Necesidad de ampliar funcionalidad sin modificar la clase base
- [ ] Interfaz incompatible
- [x] Otro: coordinación transversal de operaciones que afectan disponibilidad.

**Explicación contextualizada en el proyecto:**

El Nivel 3 identifica `ActualizacionDisponibilidad` como responsabilidad transversal y muestra que `GestionPrestamos`, `GestionDevoluciones` y `GestionIncidencias` dependen de ella. El Nivel 4 añade `ActualizadorDisponibilidad`, `PoliticaCierrePrestamo` y repositorios que deben participar en la misma coordinación.

### 5.4 Relación con el MVP

- **¿Por qué este problema sí debe resolverse en el MVP y no en una versión futura?** Porque RF-13 y RF-18 exigen que la disponibilidad sea consistente tanto al prestar como al devolver o resolver una incidencia.
- **¿Qué riesgo habría si no se aplica este patrón en el MVP?** Una devolución podría liberar una unidad que aún tiene una incidencia pendiente, o un flujo podría cerrar el préstamo sin actualizar correctamente el inventario.
- **¿La implementación del patrón es razonable para el tiempo y complejidad del MVP?** [x] Sí [ ] No

**Justificación:**

El mediador se implementa como el servicio transversal ya previsto en C4. Su responsabilidad queda limitada a coordinar cambios, no a reemplazar las reglas propias de cada componente.

### 5.5 Análisis de adecuación del patrón

- **¿Qué parte del sistema cambia o podría cambiar?** La coordinación entre una operación sobre préstamo, la condición de devolución/incidencia, el estado de la unidad y el cierre del préstamo.
- **¿Qué se desea desacoplar?** `GestionPrestamos`, `GestionDevoluciones` y `GestionIncidencias` entre sí y respecto de los detalles de actualización de inventario.
- **¿Qué ventaja concreta ofrece este patrón en este proyecto?** Centraliza la consistencia transversal y evita duplicar `reservar`, `liberar`, `marcarNoDisponible` y `cerrarSiCompleto` en varios servicios.
- **¿Qué principio de diseño ayuda a cumplir?** [x] Responsabilidad Única [x] Inversión de Dependencias [ ] Abierto/Cerrado [ ] Segregación de Interfaces [ ] Otro: —

**Justificación técnica:**

Los componentes notifican al mediador la operación realizada y este coordina los repositorios y la política de cierre dentro del límite transaccional de la API.

### 5.6 Alternativas consideradas

| Patrón alternativo | ¿Por qué podría confundirse? | ¿Por qué no es el mejor en este caso? |
|---|---|---|
| Observer | Podría notificar cambios de disponibilidad a otros interesados. | El MVP requiere coordinación transaccional inmediata, no notificaciones desacopladas o eventos externos. |
| Facade | También puede ofrecer una entrada única a varios servicios. | Facade simplifica al cliente externo; aquí se necesita coordinar colegas internos que participan en una misma operación. |

### 5.7 No repetición ni solapamiento

- **¿Este patrón ya fue usado en otra parte del proyecto?** [ ] Sí [x] No
- **¿Su propósito funcional se solapa con otro patrón seleccionado?** [ ] Sí [x] No
- **Explicación:** Mediator coordina componentes internos y Facade presenta una interfaz simplificada al controlador. State sigue siendo responsable únicamente del comportamiento según el estado de cada unidad.

### 5.8 Aplicación concreta al diseño

| Rol del patrón | Clase/componente del proyecto |
|---|---|
| Mediator | `ActualizadorDisponibilidad` |
| Colega de préstamo | `PrestamoServicio` / `GestionPrestamos` |
| Colega de devolución | `DevolucionServicio` / `GestionDevoluciones` |
| Colega de incidencia | `IncidenciaServicio` / `GestionIncidencias` |

**Descripción breve de cómo funcionará en el sistema:**

Los servicios de préstamo, devolución e incidencia solicitarán al mediador las operaciones de reservar, liberar o dejar no disponible una unidad. El mediador consultará las entidades y repositorios necesarios, invocará `PoliticaCierrePrestamo` y mantendrá la operación dentro de una transacción.

**Fragmento de pseudocódigo, diagrama o referencia UML:**

```text
class ActualizadorDisponibilidad {
    reservar(unidades)
    liberar(unidad, condicion)
    registrarIncidencia(unidad, tipo)
    cerrarSiCompleto(prestamo)
}

GestionDevoluciones -> ActualizadorDisponibilidad
GestionIncidencias  -> ActualizadorDisponibilidad
GestionPrestamos    -> ActualizadorDisponibilidad
```

### 5.9 Conclusión de decisión

- **Decisión final sobre este patrón:** [x] Se aprueba [ ] Se descarta
- **Justificación final:** El modelo C4 identifica explícitamente una responsabilidad transversal para disponibilidad. Mediator evita dependencias cruzadas y protege la consistencia exigida por el MVP.

## 6. Consolidado final de los patrones seleccionados

| Nº | Patrón GoF | Categoría | Módulo/Funcionalidad | Problema que resuelve | Justificación en el MVP | ¿Se repite? |
|---|---|---|---|---|---|---|
| 1 | State | Comportamiento | Estados del inventario | Comportamiento y transiciones variables de una unidad física | Protege disponibilidad, mantenimiento, baja y exclusividad del préstamo | No |
| 2 | Chain of Responsibility | Comportamiento | Elegibilidad de préstamos | Muchas validaciones acopladas en un único flujo | Hace mantenibles las reglas RN-01, RN-02, RN-04 y RN-05 | No |
| 3 | Strategy | Comportamiento | Sanciones por retraso | Variación del cálculo según justificación | Encapsula RN-03 sin modificar el flujo de devolución | No |
| 4 | Facade | Estructural | Registro de préstamos | Controlador acoplado a múltiples subsistemas | Mantiene una entrada simple y estable para el caso de uso principal | No |
| 5 | Mediator | Comportamiento | Disponibilidad, devoluciones e incidencias | Dependencias cruzadas y coordinación duplicada | Centraliza la consistencia entre inventario, préstamo e incidencia | No |

## 7. Validación global de consistencia

| Criterio | Sí | No | Observación |
|---|---|---|---|
| Cada patrón responde a un problema real del sistema | ☒ | ☐ | Los problemas se relacionan con reglas, componentes y elementos del Nivel 4. |
| Los 5 patrones son distintos entre sí | ☒ | ☐ | No se repite ningún nombre de patrón. |
| Ningún patrón fue incluido solo para cumplir con el número | ☒ | ☐ | Cada patrón tiene una necesidad explícita y una decisión de aprobación. |
| Todos los patrones caben en el alcance del MVP | ☒ | ☐ | Se aplican a préstamo, inventario, devolución, incidencia y sanciones ya incluidos. |
| La selección evita sobreingeniería | ☒ | ☐ | Las extensiones propuestas son interfaces, estados y validadores mínimos. |
| Existe coherencia entre problema, solución y diseño | ☒ | ☐ | Las responsabilidades corresponden a los componentes y clases definidos en C4. |

## 8. Conclusión general del análisis

Los cinco patrones seleccionados son adecuados porque responden a problemas técnicos que ya aparecen en el contexto y en el Nivel 4 del modelo C4. State protege el ciclo de vida de las unidades físicas; Chain of Responsibility organiza las validaciones críticas del préstamo; Strategy separa la política de sanciones; Facade mantiene simple la entrada al caso de uso de préstamo; y Mediator centraliza la coordinación de disponibilidad entre préstamos, devoluciones e incidencias.

La selección no introduce integraciones externas, funcionalidades nuevas ni patrones sin una necesidad concreta. Las extensiones propuestas son mínimas y pueden implementarse dentro de la API Java/Spring Boot y del modelo de dominio ya documentado. Además, la separación entre los patrones evita que una misma responsabilidad se resuelva dos veces: State administra estados, Chain valida, Strategy calcula políticas, Facade simplifica el acceso y Mediator coordina componentes.
