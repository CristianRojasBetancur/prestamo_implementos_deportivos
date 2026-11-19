/**
 * Manejo transversal de errores: {@code ManejadorGlobalErrores}
 * ({@code @RestControllerAdvice}) traduce toda excepción a
 * {@code ProblemaRespuesta} ({@code application/problem+json}) con un
 * {@code codigo} estable; {@code DelegacionErroresSeguridad} hace lo mismo con
 * los 401/403 de Spring Security. Contrato: docs/specs/BE-01-A-contratos-y-errores.md.
 */
package co.edu.itm.prestamosdeportivos.web.error;
