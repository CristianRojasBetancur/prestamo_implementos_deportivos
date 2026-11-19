package co.edu.itm.prestamosdeportivos.seguridad;

import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Rol;

/**
 * Principal de una solicitud autenticada. El rol proviene de la base de datos
 * en cada solicitud, nunca de un valor enviado por el cliente. Se obtiene en los
 * controladores con {@code @AuthenticationPrincipal UsuarioAutenticado usuario}.
 */
public record UsuarioAutenticado(UUID id, Rol rol) {
}
