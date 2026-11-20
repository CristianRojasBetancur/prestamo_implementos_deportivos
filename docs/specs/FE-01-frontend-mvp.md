# FE-01 — Aplicación web del MVP

## Objetivo

Inicializar `apps/frontend/` como aplicación React/Vite y materializar la interfaz
del MVP descrita por los componentes frontend del modelo C4. La interfaz será
responsive desde 360 px y separará presentación, sesión, rutas y comunicación
HTTP.

## Alcance

- Implementación visual fiel al archivo Figma **Sistema de Prestamo de
  Implementos Deportivos - MVP**, en particular sus páginas 00 Foundations,
  02 User, 03 Auth, 04 Prestador, 05 Administrador y 06 Responsive.
- Catálogo público de implementos; formularios de acceso; shells separados por
  rol; inicio, tablas, filtros, formularios y flujos de préstamo/devolución/
  incidencia que aparecen en el prototipo.
- Vistas enfocadas en los actores y responsabilidades definidos en el contexto
  del proyecto y en `docs/source/modelo_c4.md`.
- Tipos de datos, adaptadores por recurso y un cliente HTTP centralizado con
  errores normalizados, preparados para integrarse con la API.
- Protección de rutas según estado de sesión/rol como ayuda de navegación.
- Diseño accesible, estados vacíos y errores claros cuando la API no esté
  disponible o no esté configurada.

## Exclusiones y límites

- No se implementa backend, persistencia de negocio ni autorización de
  seguridad; la autenticación real y el consumo de API se implementan según
  FE-02.
- La reproducción visual inicial fue validada con datos de demostración. La
  integración operativa y el reemplazo de esos datos por contratos reales se
  define en `FE-02-integracion-api-frontend.md`.
- No se asumen endpoints, payloads, credenciales ni reglas que no estén en una
  especificación de API.
- Las reglas de negocio y la autorización real corresponden al backend.
- La protección de rutas del cliente no reemplaza autorización del servidor.

## Actores y vistas

- Usuario: consulta disponibilidad y filtros por categoría.
- Administrador: consulta, registra, actualiza y desactiva prestadores; consulta,
  registra, actualiza y retira implementos.
- Prestador: busca y, si es necesario, registra solicitantes operativos; consulta
  su situación y préstamos; registra préstamos, devoluciones e incidencias y
  consulta historial.
- Autenticación: presenta acceso y estado de sesión, sin simular validación real.

Las operaciones que requieran API sin contrato disponible deben mostrar un
estado de servicio no configurado/no disponible y no simular éxito.

## Arquitectura y contratos

- React con Vite y TypeScript.
- Módulos de UI separados por flujo funcional.
- Cliente HTTP único; adaptadores por recurso con tipos de entrada y salida.
- URL base configurada por `VITE_API_BASE_URL`; errores HTTP normalizados en un
  único punto.
- Las pantallas internas requieren una sesión válida de la API. Sin API
  configurada o disponible se muestran estados vacíos/error y no se confirman
  operaciones.

## Sistema visual Figma

- Fondo de página `#F8FAFC`, superficies blancas y texto oscuro.
- Acción primaria azul `#2563EB`, hover azul más oscuro; estados verde para
  éxito/disponible, rojo para error/daño, ámbar para advertencia y gris para
  estados neutros.
- Tipografía Inter; título de página 24 px, encabezados secundarios 20 px,
  cuerpo 16 px, cuerpo pequeño 14 px, etiquetas 14 px y captions 12 px.
- Espaciado basado en 4/8/12/16/24/32/40/48 px, radios pequeños/medianos/grandes.
- Shell administrativo y operativo con sidebar, header superior, área central
  clara, filtros separados de tablas y botones de acción; catálogo público sin
  sidebar, con header institucional y tarjetas en tres columnas.
- En móvil los filtros y tarjetas pasan a una columna y la navegación se reduce
  al header compacto definido en la página Responsive.

## Criterios de aceptación

1. `apps/frontend/` contiene una aplicación React/Vite ejecutable con comandos
   documentados.
2. Existe navegación responsive a las seis áreas descritas en esta especificación.
3. El catálogo presenta los seis implementos y estados del frame Figma de
   resultados, permite buscar y filtrar por categoría, y representa carga,
   vacío y sin resultados.
4. Las tablas, formularios y flujos por rol conservan los campos, jerarquías,
   acciones, pasos y estados que aparecen en los frames de Figma.
5. Las operaciones autorizadas consumen la API y no presentan una operación
   como exitosa si la respuesta no fue exitosa.
6. Comunicación HTTP, tipos y errores están aislados de las vistas.
7. La UI es usable con teclado, etiquetas accesibles y soporte desde 360 px.
8. Hay pruebas para navegación, catálogo, operaciones autorizadas y errores.
9. No hay conexiones directas desde el frontend a PostgreSQL ni reglas de
   negocio autoritativas en cliente.

## Verificación

- Pruebas de componentes para navegación, catálogo y normalización de errores.
- Build de producción de Vite.
- Revisión manual de los flujos visibles y del layout responsive.

## Trazabilidad

- Fuente funcional/arquitectónica: contexto MVP y componentes de aplicación web
  en `docs/source/modelo_c4.md` (C4 Nivel 3 y Nivel 4).
- Convenciones técnicas: `apps/frontend/AGENTS.md` y
  `docs/agent-context/arquitectura-tecnica.md`.
