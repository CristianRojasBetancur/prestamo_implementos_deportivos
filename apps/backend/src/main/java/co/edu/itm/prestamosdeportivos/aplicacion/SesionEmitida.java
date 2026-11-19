package co.edu.itm.prestamosdeportivos.aplicacion;

import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.seguridad.TokenEmitido;

/** Resultado de un inicio de sesión exitoso. */
public record SesionEmitida(Usuario usuario, TokenEmitido token) {
}
