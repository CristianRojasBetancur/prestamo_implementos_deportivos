package co.edu.itm.prestamosdeportivos.seguridad;

import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Convierte un JWT ya validado (firma, expiración, emisor) en la autenticación
 * de la solicitud, verificando contra la base de datos que:
 * <ol>
 *   <li>el usuario exista, esté activo y tenga acceso (no USER);</li>
 *   <li>el claim {@code ver} coincida con su versión de credenciales
 *       (revocación por cierre de sesión o desactivación).</li>
 * </ol>
 * La autoridad {@code ROLE_<rol>} se toma de la base de datos, no del token.
 */
@Component
public class ConversorJwtAUsuario implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String SESION_NO_VALIDA = "La sesión no es válida.";

    private final UsuarioRepositorio usuarios;

    public ConversorJwtAUsuario(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Usuario usuario = usuarios.findById(identificador(jwt))
                .orElseThrow(() -> new SesionInvalidaException(SESION_NO_VALIDA));
        if (!usuario.puedeIniciarSesion() || !versionVigente(jwt, usuario)) {
            throw new SesionInvalidaException(SESION_NO_VALIDA);
        }
        var principal = new UsuarioAutenticado(usuario.getId(), usuario.getRol());
        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, autoridades);
    }

    private static UUID identificador(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new SesionInvalidaException(SESION_NO_VALIDA);
        }
    }

    private static boolean versionVigente(Jwt jwt, Usuario usuario) {
        Object version = jwt.getClaims().get(EmisorTokens.CLAIM_VERSION);
        return version instanceof Number numero && numero.intValue() == usuario.getVersionCredenciales();
    }
}
