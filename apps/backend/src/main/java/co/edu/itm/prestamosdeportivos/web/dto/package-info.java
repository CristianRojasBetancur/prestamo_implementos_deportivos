/**
 * DTO de solicitud y respuesta de cada caso de uso de BE-01 §10, agrupados por
 * recurso como records anidados (por ejemplo {@code PrestamoDto.CrearPrestamoSolicitud}).
 *
 * <ul>
 *   <li>Las entidades JPA nunca se serializan: los controladores convierten
 *       entidades a estos records.</li>
 *   <li>Ningún DTO de respuesta contiene contraseñas, hashes, versiones
 *       internas ni tokens (salvo {@code RespuestaLogin.tokenAcceso}).</li>
 *   <li>Las fechas de respuesta son {@code OffsetDateTime} en la zona
 *       institucional (ver {@code ConversorFechas}).</li>
 *   <li>Los mensajes de validación están en español y se reutilizan desde
 *       {@link co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion}.</li>
 * </ul>
 */
package co.edu.itm.prestamosdeportivos.web.dto;
