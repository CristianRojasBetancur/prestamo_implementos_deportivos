# SOLID y patrones de diseño

Este documento resume las decisiones de diseño aprobadas en el contexto del proyecto. Los patrones no son requisitos funcionales ni deben aplicarse automáticamente. Una especificación SDD puede restringir, reemplazar o extender esta decisión de forma explícita.

Debe mantenerse actualizado cuando se adopten, retiren o reubiquen patrones, cambien las convenciones técnicas o la implementación real demuestre que una decisión ya no aplica. Las decisiones obsoletas deben corregirse en este contexto sin alterar los documentos fuente históricos.

## Principios SOLID

- **Responsabilidad única (SRP):** cada clase, componente o módulo debe tener una razón principal para cambiar.
- **Abierto/cerrado (OCP):** extender comportamientos mediante abstracciones y composiciones, evitando modificar código estable sin necesidad.
- **Sustitución de Liskov (LSP):** las implementaciones deben respetar el contrato de la abstracción que sustituyen.
- **Segregación de interfaces (ISP):** preferir contratos pequeños y específicos en lugar de interfaces grandes y forzadas.
- **Inversión de dependencias (DIP):** las políticas de alto nivel dependen de abstracciones; los detalles se conectan mediante adaptadores.

SOLID no justifica crear capas o interfaces innecesarias. La complejidad debe estar respaldada por una variación real, una regla crítica o una necesidad de prueba.

## Patrones aprobados

| Patrón | Aplicación prevista | Responsabilidad |
|---|---|---|
| State | Estados de `ImplementoDeportivo`. | Encapsular acciones y transiciones según disponibilidad, préstamo, mantenimiento, baja o incidencia. |
| Chain of Responsibility | Elegibilidad del préstamo. | Ejecutar validaciones independientes y ordenables antes de registrar. |
| Strategy | Sanciones por retraso. | Encapsular algoritmos de cálculo según justificación y política aplicable. |
| Facade | Registro de préstamos. | Ofrecer al controlador una entrada estable frente a varios subsistemas. |
| Mediator | Disponibilidad, préstamos, devoluciones e incidencias. | Coordinar cambios transversales y evitar dependencias cruzadas. |

## Límites de aplicación

- `State` no sustituye las validaciones de una solicitud ni debe convertirse en un motor de reglas general.
- `Chain of Responsibility` valida una solicitud; no modela el ciclo de vida persistente de una unidad.
- `Strategy` selecciona una política o algoritmo; no debe usarse para ocultar una única fórmula trivial.
- `Facade` simplifica la entrada de un cliente externo; no debe absorber todas las responsabilidades del dominio.
- `Mediator` coordina colegas internos; no debe convertirse en un servicio monolítico que conozca toda la aplicación.

## Criterio obligatorio antes de introducir otro patrón

El agente debe demostrar en la especificación o en la decisión técnica:

1. el problema concreto existente;
2. la variación o acoplamiento que se quiere controlar;
3. por qué una solución directa no es suficiente;
4. cómo se evita solapamiento con los patrones existentes;
5. cómo se mantiene el alcance del MVP.

