package co.edu.itm.prestamosdeportivos.seguridad;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite el JWT de sesión. Claims mínimos, sin datos personales:
 * {@code iss}, {@code sub} (UUID), {@code iat}, {@code exp}, {@code jti},
 * {@code rol} (informativo para el cliente) y {@code ver} (versión de
 * credenciales, para revocación). El backend no autoriza con el claim
 * {@code rol}: recarga el rol desde la base de datos.
 */
@Component
public class EmisorTokens {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_VERSION = "ver";

    private final JwtEncoder codificador;
    private final PropiedadesJwt propiedades;
    private final Clock reloj;

    public EmisorTokens(JwtEncoder codificador, PropiedadesJwt propiedades, Clock reloj) {
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant expiracion = ahora.plus(propiedades.expiracion());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim(CLAIM_VERSION, usuario.getVersionCredenciales())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenEmitido(valor, expiracion);
    }
}
