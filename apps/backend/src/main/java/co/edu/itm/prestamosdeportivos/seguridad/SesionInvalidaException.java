package co.edu.itm.prestamosdeportivos.seguridad;

import org.springframework.security.core.AuthenticationException;

/**
 * El token es válido criptográficamente, pero la sesión ya no lo es: usuario
 * inexistente, inactivo, sin acceso o con sesiones revocadas. Se responde
 * {@code 401 SESION_INVALIDA}.
 */
public class SesionInvalidaException extends AuthenticationException {

    public SesionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
