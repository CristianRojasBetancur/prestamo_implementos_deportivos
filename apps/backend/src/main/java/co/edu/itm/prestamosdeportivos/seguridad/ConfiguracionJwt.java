package co.edu.itm.prestamosdeportivos.seguridad;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Codificador y decodificador JWT HS256 con la misma clave simétrica. El
 * decodificador valida firma, algoritmo, {@code exp}/{@code nbf} (tolerancia de
 * 60 s) y el emisor {@code iss}.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionJwt {

    private static final String ALGORITMO_HMAC = "HmacSHA256";

    @Bean
    SecretKey claveFirmaJwt(PropiedadesJwt propiedades) {
        return new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC);
    }

    @Bean
    JwtEncoder codificadorJwt(SecretKey claveFirmaJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveFirmaJwt));
    }

    @Bean
    JwtDecoder decodificadorJwt(SecretKey claveFirmaJwt, PropiedadesJwt propiedades) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveFirmaJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.emisor()));
        return decodificador;
    }
}
