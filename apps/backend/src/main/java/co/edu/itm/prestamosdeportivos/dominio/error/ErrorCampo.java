package co.edu.itm.prestamosdeportivos.dominio.error;

/**
 * Error asociado a un campo o elemento de la solicitud. {@code campo} usa la
 * notación del JSON recibido, por ejemplo {@code items[1].incidencia}.
 */
public record ErrorCampo(String campo, String mensaje) {
}
