# Plan de implementación del modelo C4

## 1. Propósito

Construir progresivamente el modelo C4 del MVP del **Sistema de Préstamo de Implementos Deportivos**, documentando las decisiones arquitectónicas en Markdown y validando la coherencia entre los niveles de contexto, contenedores, componentes y código.

Este archivo constituye únicamente el plan de trabajo. En esta etapa no se modificará `modelo_c4.md`, no se crearán diagramas y no se crearán archivos `.dsl`.

## 2. Alcance arquitectónico

El modelo se limitará al contexto definido en `contexto_proyecto_sistemas_de_informacion.pdf` y a las historias de usuario del MVP.

### Actores del sistema

- **Usuario:** consulta la disponibilidad de implementos por categoría.
- **Administrador:** administra prestadores e inventario.
- **Prestador:** registra y consulta préstamos, devoluciones e incidencias.

### Responsabilidad principal de la plataforma

Automatizar y dar trazabilidad al ciclo de vida del préstamo de implementos deportivos: autenticación, administración del inventario, registro del préstamo, actualización de disponibilidad, devolución y gestión de incidencias.

### Información administrada

- Usuarios y roles.
- Prestadores habilitados o deshabilitados.
- Implementos, categorías, estados y disponibilidad.
- Préstamos, fechas y responsables.
- Devoluciones y condiciones de recepción.
- Incidencias por daño, pérdida, robo u otra novedad.

### Sistemas externos

No se modelarán ERP, servicios de mapas, geolocalización, notificaciones externas ni otros sistemas de terceros porque no están definidos en el contexto del MVP.

## 3. Decisiones tecnológicas de referencia

Para que los niveles 2, 3 y 4 tengan una base concreta, se utilizará la siguiente propuesta tecnológica:

| Área                          | Tecnología propuesta  |
| ----------------------------- | --------------------- |
| Aplicación web                | React con Vite        |
| API backend                   | Java con Spring Boot  |
| Persistencia                  | PostgreSQL            |
| Comunicación frontend-backend | HTTP/JSON sobre HTTPS |

Estas tecnologías son una propuesta para el modelo arquitectónico del laboratorio. No implican que el proyecto deba implementarse inmediatamente con ellas.

## 4. Entregables previstos

En las fases posteriores se completará el archivo existente `modelo_c4.md` y se podrá reorganizar el proyecto de acuerdo con la guía de Markdown.

La estructura prevista será:

```text
.
├── PLAN_IMPLEMENTACION.md
├── modelo_c4.md
├── diagramas/
└── estructurizr/
```

La carpeta `estructurizr/` se reservará para el código fuente de Structurizr DSL cuando se implemente cada fase. No forma parte de esta primera etapa.

Los diagramas podrán representarse mediante bloques Mermaid en el Markdown o mediante imágenes exportadas, según la decisión que se tome al ejecutar las fases. En ambos casos se conservarán las fuentes y rutas de forma reproducible.

## 5. Fase 1 — C4 Nivel 1: Contexto

### Objetivo

Representar el sistema como una caja negra y mostrar quién interactúa con él.

### Actividades

1. Completar la sección **Comprensión del problema** del archivo `modelo_c4.md`.
2. Documentar el problema actual, sus causas, impactos y flujo manual.
3. Identificar los tres actores del MVP y sus responsabilidades.
4. Definir el límite de responsabilidad de la plataforma.
5. Construir el diagrama de contexto del sistema.
6. Responder por qué no se incluyen base de datos, API, componentes, clases ni frameworks en este nivel.
7. Incorporar el diagrama y su explicación en la sección `C4 Nivel 1 - Contexto`.

### Elementos obligatorios del diagrama

- Usuario.
- Administrador.
- Prestador.
- Sistema de Préstamo de Implementos Deportivos.

### Relaciones mínimas

- Usuario consulta la disponibilidad de implementos.
- Administrador administra prestadores e inventario.
- Prestador gestiona préstamos, devoluciones e incidencias.

### Criterios de aceptación

- El sistema aparece como un único software system.
- Los actores están fuera del límite del sistema.
- No aparecen detalles internos de implementación.
- Cada relación coincide con el alcance funcional del MVP.
- No se agregan sistemas externos no definidos en el contexto.

## 6. Fase 2 — C4 Nivel 2: Contenedores

### Objetivo

Descomponer la plataforma en unidades ejecutables o almacenes de datos con responsabilidades claras.

### Contenedores definidos

| Contenedor         | Responsabilidad                                                       | Tecnología       | Comunicación                                             |
| ------------------ | --------------------------------------------------------------------- | ---------------- | -------------------------------------------------------- |
| Aplicación web MVP | Presentar las funcionalidades según el rol del usuario                | React/Vite       | Recibe solicitudes de los actores y consume la API       |
| API backend        | Aplicar autenticación, autorización y reglas del negocio              | Java/Spring Boot | Expone operaciones HTTP/JSON y consulta la base de datos |
| Base de datos      | Persistir usuarios, inventario, préstamos, devoluciones e incidencias | PostgreSQL       | Es accedida únicamente por la API backend                |

### Actividades

1. Definir la responsabilidad y la frontera de cada contenedor.
2. Documentar la tecnología propuesta y el motivo de su uso.
3. Modelar el flujo de comunicación entre actores, aplicación web, API y base de datos.
4. Construir el diagrama de contenedores.
5. Responder qué criterio justifica que cada elemento sea un contenedor independiente.
6. Relacionar los contenedores con los RF y RNF del contexto.
7. Incorporar el diagrama y las tablas en la sección `C4 Nivel 2 - Contenedores`.

### Criterios de aceptación

- La aplicación web no accede directamente a PostgreSQL.
- La API concentra las reglas de negocio y las validaciones.
- La base de datos se representa como almacén de datos.
- Las comunicaciones indican el protocolo o mecanismo principal.
- Los contenedores explican la estructura de despliegue lógico del MVP, no clases internas.
- Se consideran los requisitos de rendimiento, seguridad, consistencia y disponibilidad sin convertirlos en contenedores.

## 7. Fase 3 — C4 Nivel 3: Componentes

### Objetivo

Descomponer la aplicación web y la API backend en responsabilidades arquitectónicas significativas.

Aunque la guía propone seleccionar un único contenedor, en este proyecto se documentarán los dos contenedores principales para mantener la trazabilidad completa del MVP.

### Componentes de la aplicación web

- **AutenticacionUI:** inicio de sesión, cierre de sesión y estado de autenticación.
- **CatalogoDisponibilidadUI:** consulta y filtrado de implementos disponibles.
- **GestionPrestadoresUI:** registro, consulta, actualización y desactivación de prestadores.
- **GestionInventarioUI:** registro, consulta, actualización y retiro de implementos.
- **OperacionPrestamosUI:** consulta del usuario, validación previa y registro del préstamo.
- **OperacionDevolucionesUI:** registro de devoluciones, condiciones e incidencias.
- **ApiClient:** comunicación centralizada con la API y manejo común de respuestas.

### Componentes de la API backend

- **AutenticacionAutorizacion:** autenticación y autorización por rol.
- **GestionPrestadores:** administración de prestadores.
- **GestionInventario:** inventario, categorías, estados y disponibilidad.
- **GestionPrestamos:** elegibilidad, límite de implementos, fechas y exclusividad.
- **GestionDevoluciones:** registro de devoluciones y actualización de fechas.
- **GestionIncidencias:** registro de daños, pérdidas, robos y novedades.
- **ActualizacionDisponibilidad:** sincronización consistente entre préstamos, devoluciones e inventario.
- **Persistencia:** acceso a PostgreSQL mediante repositorios.

### Actividades

1. Relacionar cada componente con las historias de usuario y requisitos funcionales correspondientes.
2. Documentar la responsabilidad y dependencias de cada componente.
3. Construir una vista de componentes para la aplicación web.
4. Construir una vista de componentes para la API backend.
5. Verificar que los componentes representen responsabilidades y no entidades aisladas del dominio.
6. Incorporar tablas, diagramas y explicación en la sección `C4 Nivel 3 - Componentes`.

### Criterios de aceptación

- Cada componente tiene una responsabilidad única y significativa.
- Los componentes del frontend no contienen reglas de persistencia.
- Los componentes del backend concentran las reglas de negocio.
- Las operaciones de préstamo, devolución e incidencia mantienen coherencia con el inventario.
- Las dependencias entre componentes son necesarias y están justificadas.
- Todos los RF del MVP tienen al menos un componente responsable.

## 8. Fase 4 — C4 Nivel 4: Código

### Objetivo

Mostrar cómo los componentes definidos en el Nivel 3 se materializan en elementos de código relevantes.

En este proyecto se cubrirán todos los componentes de frontend y backend, no únicamente un componente de ejemplo.

### Elementos de código del frontend

Para cada componente frontend se documentarán los elementos representativos que correspondan, por ejemplo:

- Páginas y componentes React.
- Formularios y tablas.
- Hooks de sesión y permisos.
- Servicios de consumo HTTP.
- Tipos o interfaces de datos.
- Protección de rutas.
- Adaptadores de respuestas y errores.

### Elementos de código del backend

Para cada componente backend se documentarán los elementos representativos que correspondan, por ejemplo:

- Controladores REST.
- Servicios de aplicación.
- Interfaces de repositorio.
- Entidades de dominio.
- DTO de entrada y salida.
- Validadores de reglas de negocio.
- Adaptadores de persistencia.
- Excepciones y manejo de errores.
- Límites transaccionales.

Se deben realizar los diagramas de clase (UML) para cada componente, siguiendo la línea de incluirlos en el archivo `modelo_c4.md`, en la sección `C4 Nivel 4 - Código` con mermaid.

### Reglas de negocio que deben evidenciarse

- Un usuario puede tener como máximo cuatro implementos en préstamo.
- Un préstamo regular dura como máximo veinticuatro horas.
- Un usuario con préstamos vencidos no puede recibir nuevos implementos.
- Una unidad física no puede estar en dos préstamos activos simultáneamente.
- Los implementos en mantenimiento o dados de baja no pueden prestarse.
- Una devolución actualiza el estado del implemento según su condición.
- Un préstamo finaliza cuando todos sus implementos fueron devueltos o resueltos mediante una incidencia.

### Actividades

1. Seleccionar clases, interfaces, servicios, repositorios, entidades, DTO y adaptadores necesarios para cada componente.
2. Documentar tipo, responsabilidad, componente de pertenencia y relación principal.
3. Construir las vistas de código que representen frontend y backend.
4. Verificar que las clases implementen la responsabilidad del componente y no introduzcan responsabilidades ajenas.
5. Incorporar las tablas, diagramas y explicación en la sección `C4 Nivel 4 - Código`.
6. Completar la sección de uso y validación crítica de IA.
7. Completar la auditoría de coherencia entre los cuatro niveles.
8. Completar las conclusiones del laboratorio.

### Criterios de aceptación

- Cada elemento de código pertenece a un componente previamente definido.
- Las clases y servicios reflejan responsabilidades reales del MVP.
- Las reglas de negocio críticas son visibles en el diseño del backend.
- El código del frontend se relaciona con los componentes de interfaz definidos en el Nivel 3.
- No se modela código sin relación con una HU, RF, RN o responsabilidad arquitectónica.

## 9. Uso crítico de IA y auditoría

Después de construir los cuatro niveles, se utilizará IA únicamente como revisor arquitectónico.

La revisión deberá buscar:

- Elementos ubicados en un nivel incorrecto.
- Inconsistencias entre niveles.
- Responsabilidades mal definidas.
- Componentes que parezcan clases.
- Clases que no correspondan al componente seleccionado.
- Dependencias innecesarias.
- Problemas de separación de responsabilidades.

El archivo final deberá registrar:

1. El prompt utilizado.
2. Al menos tres observaciones generadas por la IA.
3. La decisión del equipo frente a cada observación.
4. La justificación de aceptación, rechazo o modificación.
5. Los hallazgos de la auditoría de coherencia y sus correcciones.

## 10. Validación global

Antes de considerar terminado el laboratorio se verificará:

- La estructura original de `modelo_c4.md` permanece intacta.
- Las ocho secciones exigidas por la guía están completas.
- Los cuatro niveles describen el mismo sistema y el mismo alcance MVP.
- No aparecen conceptos propios del caso LogiTrack que no existan en el proyecto.
- La cadena `actor → contenedor → componente → código` puede seguirse para cada funcionalidad relevante.
- Las rutas de diagramas y recursos usan nombres claros y rutas relativas.
- Los diagramas son legibles desde la vista previa de Markdown.
- Las decisiones tecnológicas están diferenciadas de los requisitos del contexto.

## 11. Estado inicial y siguiente orden de trabajo

Estado actual:

- `modelo_c4.md` existe con su estructura base.
- Este archivo contiene el plan de implementación.
- No se han creado archivos `.dsl`.
- No se han creado diagramas.
- No se ha modificado el entregable principal.

Orden posterior de implementación:

1. Ejecutar la Fase 1 y completar el Nivel 1.
2. Ejecutar la Fase 2 y completar el Nivel 2.
3. Ejecutar la Fase 3 y completar el Nivel 3.
4. Ejecutar la Fase 4 y completar el Nivel 4, la revisión con IA, la auditoría y las conclusiones.
