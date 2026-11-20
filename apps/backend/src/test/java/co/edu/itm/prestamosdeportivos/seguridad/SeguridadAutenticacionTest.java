package co.edu.itm.prestamosdeportivos.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.crypto.spec.SecretKeySpec;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Suite de seguridad sobre HTTP real (RF-01, RF-02, RNF-03): inicio de sesión,
 * tokens inválidos, expirados y revocados, y matriz de roles contra cada grupo
 * de endpoints de BE-01 §9. Las cuentas son exclusivas de pruebas y sus
 * contraseñas no se usan en ningún entorno real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
class SeguridadAutenticacionTest {

    // Cuentas exclusivas de pruebas (dominio .invalid, reservado y no enrutable).
    static final Cuenta ADMIN = new Cuenta("SEC-ADMIN", "admin.seguridad@pruebas.invalid", Rol.ADMIN, "Prueba#Admin-2026");
    static final Cuenta PRESTADOR = new Cuenta("SEC-PREST", "prestador.seguridad@pruebas.invalid", Rol.PRESTADOR,
            "Prueba#Prestador-2026");
    static final Cuenta INACTIVO = new Cuenta("SEC-INACT", "inactivo.seguridad@pruebas.invalid", Rol.PRESTADOR,
            "Prueba#Inactivo-2026");
    static final Cuenta CIERRE = new Cuenta("SEC-LOGOUT", "logout.seguridad@pruebas.invalid", Rol.PRESTADOR,
            "Prueba#Logout-2026");
    static final Cuenta REVOCACION = new Cuenta("SEC-REVOC", "revocacion.seguridad@pruebas.invalid", Rol.PRESTADOR,
            "Prueba#Revocacion-2026");
    static final String SOLICITANTE = "SEC-USER";

    private static final Pattern TOKEN = Pattern.compile("\"tokenAcceso\":\"([^\"]+)\"");
    private static final Pattern DETALLE = Pattern.compile("\"detail\":\"([^\"]+)\"");
    private static final Map<Rol, String> TOKENS_MATRIZ = new HashMap<>();

    private final HttpClient cliente = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private PasswordEncoder codificador;

    @Autowired
    private JwtEncoder codificadorJwt;

    @Autowired
    private PropiedadesJwt propiedadesJwt;

    @BeforeEach
    void cuentasDePrueba() {
        for (Cuenta cuenta : new Cuenta[] {ADMIN, PRESTADOR, INACTIVO, CIERRE, REVOCACION}) {
            asegurar(cuenta, cuenta != INACTIVO);
        }
        if (!usuarios.existsByIdentificacion(SOLICITANTE)) {
            usuarios.save(Usuario.solicitante(SOLICITANTE, "Solicitante", "Pruebas"));
        }
    }

    // ------------------------------------------------------------------ login

    @Nested
    @DisplayName("Inicio de sesión")
    class InicioSesion {

        @Test
        @DisplayName("Un ADMIN activo recibe token, tipo Bearer, expiración y su rol")
        void loginExitoso() throws Exception {
            HttpResponse<String> respuesta = login(ADMIN.correo(), ADMIN.contrasena());

            assertThat(respuesta.statusCode()).isEqualTo(200);
            assertThat(respuesta.body()).contains("\"tipoToken\":\"Bearer\"", "\"rol\":\"ADMIN\"", "\"expiraEn\"");
            assertThat(respuesta.body()).doesNotContain("contrasena", "hash", "versionCredenciales");
        }

        @Test
        @DisplayName("El token solo contiene claims mínimos, sin datos personales")
        void tokenSinDatosSensibles() throws Exception {
            String token = extraerToken(login(PRESTADOR.correo(), PRESTADOR.contrasena()));
            String carga = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

            assertThat(carga).contains("\"sub\"", "\"exp\"", "\"iat\"", "\"iss\"", "\"rol\"", "\"ver\"");
            assertThat(carga).doesNotContain(PRESTADOR.correo(), PRESTADOR.identificacion(), "Pruebas", "contrasena");
        }

        @Test
        @DisplayName("Se puede iniciar sesión con la identificación")
        void loginPorIdentificacion() throws Exception {
            HttpResponse<String> respuesta = login(PRESTADOR.identificacion(), PRESTADOR.contrasena());

            assertThat(respuesta.statusCode()).isEqualTo(200);
            assertThat(respuesta.body()).contains("\"rol\":\"PRESTADOR\"");
        }

        @Test
        @DisplayName("Contraseña incorrecta y usuario inexistente responden igual (no revelan cuál falló)")
        void credencialesIncorrectas() throws Exception {
            HttpResponse<String> contrasenaMala = login(PRESTADOR.correo(), "contrasena-incorrecta");
            HttpResponse<String> usuarioInexistente = login("no-existe@pruebas.invalid", PRESTADOR.contrasena());

            for (HttpResponse<String> respuesta : java.util.List.of(contrasenaMala, usuarioInexistente)) {
                assertThat(respuesta.statusCode()).isEqualTo(401);
                assertThat(respuesta.body()).contains("\"codigo\":\"CREDENCIALES_INVALIDAS\"");
            }
            assertThat(detalle(contrasenaMala)).isEqualTo(detalle(usuarioInexistente));
        }

        @Test
        @DisplayName("Un usuario inactivo no puede iniciar sesión, aunque la contraseña sea correcta")
        void usuarioInactivo() throws Exception {
            HttpResponse<String> respuesta = login(INACTIVO.correo(), INACTIVO.contrasena());

            assertThat(respuesta.statusCode()).isEqualTo(401);
            assertThat(respuesta.body()).contains("\"codigo\":\"CREDENCIALES_INVALIDAS\"");
        }

        @Test
        @DisplayName("Un solicitante (USER) no tiene acceso al sistema")
        void solicitanteSinAcceso() throws Exception {
            HttpResponse<String> respuesta = login(SOLICITANTE, "cualquier-contrasena");

            assertThat(respuesta.statusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("El rol enviado por el cliente se ignora: lo determina el servidor")
        void rolDelClienteIgnorado() throws Exception {
            String cuerpo = "{\"identificador\":\"" + PRESTADOR.correo() + "\",\"contrasena\":\""
                    + PRESTADOR.contrasena() + "\",\"rol\":\"ADMIN\"}";
            HttpResponse<String> respuesta = enviar(json("POST", "/api/v1/auth/login", cuerpo, null));

            assertThat(respuesta.statusCode()).isEqualTo(200);
            assertThat(respuesta.body()).contains("\"rol\":\"PRESTADOR\"").doesNotContain("\"rol\":\"ADMIN\"");
        }

        @Test
        @DisplayName("Campos vacíos responden 400 con errores por campo")
        void camposVacios() throws Exception {
            HttpResponse<String> respuesta = enviar(json("POST", "/api/v1/auth/login", "{}", null));

            assertThat(respuesta.statusCode()).isEqualTo(400);
            assertThat(respuesta.body()).contains("\"codigo\":\"VALIDACION_FALLIDA\"", "identificador", "contrasena");
        }
    }

    // ---------------------------------------------------------------- tokens

    @Nested
    @DisplayName("Sesión y tokens")
    class SesionYTokens {

        @Test
        @DisplayName("/auth/me devuelve el usuario autenticado con un token válido")
        void sesionActual() throws Exception {
            String token = tokenDe(PRESTADOR);

            HttpResponse<String> respuesta = enviar(peticion("GET", "/api/v1/auth/me", token));

            assertThat(respuesta.statusCode()).isEqualTo(200);
            assertThat(respuesta.body()).contains("\"rol\":\"PRESTADOR\"", PRESTADOR.correo());
        }

        @Test
        @DisplayName("Sin token: 401 NO_AUTENTICADO")
        void sinToken() throws Exception {
            HttpResponse<String> respuesta = enviar(peticion("GET", "/api/v1/auth/me", null));

            assertThat(respuesta.statusCode()).isEqualTo(401);
            assertThat(respuesta.body()).contains("\"codigo\":\"NO_AUTENTICADO\"");
        }

        @Test
        @DisplayName("Token mal formado: 401 SESION_INVALIDA")
        void tokenMalFormado() throws Exception {
            assertSesionInvalida("abc.def.ghi");
        }

        @Test
        @DisplayName("Token expirado: 401 SESION_INVALIDA")
        void tokenExpirado() throws Exception {
            Usuario prestador = usuarios.findByIdentificacion(PRESTADOR.identificacion()).orElseThrow();
            Instant hace3Horas = Instant.now().minus(3, ChronoUnit.HOURS);
            String token = firmar(codificadorJwt, prestador, hace3Horas, hace3Horas.plus(1, ChronoUnit.HOURS),
                    propiedadesJwt.emisor());

            assertSesionInvalida(token);
        }

        @Test
        @DisplayName("Token firmado con otra clave: 401 SESION_INVALIDA")
        void tokenConOtraClave() throws Exception {
            Usuario prestador = usuarios.findByIdentificacion(PRESTADOR.identificacion()).orElseThrow();
            var otraClave = new SecretKeySpec("una-clave-distinta-de-al-menos-32-bytes!!".getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            JwtEncoder otroCodificador = new NimbusJwtEncoder(new ImmutableSecret<>(otraClave));
            Instant ahora = Instant.now();

            assertSesionInvalida(firmar(otroCodificador, prestador, ahora, ahora.plus(10, ChronoUnit.MINUTES),
                    propiedadesJwt.emisor()));
        }

        @Test
        @DisplayName("Token con otro emisor: 401 SESION_INVALIDA")
        void tokenConOtroEmisor() throws Exception {
            Usuario prestador = usuarios.findByIdentificacion(PRESTADOR.identificacion()).orElseThrow();
            Instant ahora = Instant.now();

            assertSesionInvalida(firmar(codificadorJwt, prestador, ahora, ahora.plus(10, ChronoUnit.MINUTES),
                    "otro-emisor"));
        }

        @Test
        @DisplayName("Alterar la carga del token (por ejemplo, el rol) invalida la firma")
        void tokenAlterado() throws Exception {
            String[] partes = tokenDe(PRESTADOR).split("\\.");
            String carga = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                    .replace("PRESTADOR", "ADMIN");
            String alterado = partes[0] + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(carga.getBytes(StandardCharsets.UTF_8)) + "." + partes[2];

            assertSesionInvalida(alterado);
        }

        @Test
        @DisplayName("Cerrar sesión revoca el token; un nuevo login funciona")
        void cierreDeSesion() throws Exception {
            String token = tokenDe(CIERRE);

            assertThat(enviar(peticion("POST", "/api/v1/auth/logout", token)).statusCode()).isEqualTo(204);
            assertSesionInvalida(token);
            assertThat(enviar(peticion("GET", "/api/v1/auth/me", tokenDe(CIERRE))).statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("Desactivar un usuario invalida de inmediato sus tokens")
        void desactivacionRevocaTokens() throws Exception {
            String token = tokenDe(REVOCACION);
            Usuario usuario = usuarios.findByIdentificacion(REVOCACION.identificacion()).orElseThrow();
            usuario.desactivar();
            usuarios.save(usuario);

            assertSesionInvalida(token);
        }
    }

    // --------------------------------------------------------- matriz de roles

    /** Grupo de endpoints de BE-01 §9 y los roles que pueden usarlo (vacío = público). */
    record Caso(String metodo, String ruta, Set<Rol> permitidos) {
        @Override
        public String toString() {
            return metodo + " " + ruta + " → " + (permitidos.isEmpty() ? "público" : permitidos);
        }
    }

    static Stream<Caso> matriz() {
        String id = UUID.randomUUID().toString();
        Set<Rol> soloAdmin = Set.of(Rol.ADMIN);
        Set<Rol> soloPrestador = Set.of(Rol.PRESTADOR);
        return Stream.of(
                new Caso("GET", "/api/v1/disponibilidad", Set.of()),
                new Caso("GET", "/api/v1/availability", Set.of()),
                new Caso("GET", "/api/v1/categorias", Set.of()),
                new Caso("GET", "/api/v1/categorias/" + id, soloAdmin),
                new Caso("POST", "/api/v1/categorias", soloAdmin),
                new Caso("PUT", "/api/v1/categorias/" + id, soloAdmin),
                new Caso("GET", "/api/v1/prestadores", soloAdmin),
                new Caso("POST", "/api/v1/prestadores", soloAdmin),
                new Caso("PATCH", "/api/v1/prestadores/" + id + "/estado", soloAdmin),
                new Caso("GET", "/api/v1/implementos", soloAdmin),
                new Caso("GET", "/api/v1/inventory/units", soloAdmin),
                new Caso("GET", "/api/v1/inventory/units/" + id, soloAdmin),
                new Caso("POST", "/api/v1/inventory/units", soloAdmin),
                new Caso("PATCH", "/api/v1/inventory/units/" + id, soloAdmin),
                new Caso("PATCH", "/api/v1/inventory/units/" + id + "/state", soloAdmin),
                new Caso("POST", "/api/v1/implementos", soloAdmin),
                new Caso("PATCH", "/api/v1/implementos/" + id + "/estado", soloAdmin),
                new Caso("GET", "/api/v1/implementos/prestables", soloPrestador),
                new Caso("GET", "/api/v1/usuarios", soloPrestador),
                new Caso("GET", "/api/v1/usuarios/" + id + "/situacion", soloPrestador),
                new Caso("GET", "/api/v1/users/" + id + "/loan-situation", soloPrestador),
                new Caso("GET", "/api/v1/prestamos", soloPrestador),
                new Caso("GET", "/api/v1/prestamos/" + id, soloPrestador),
                new Caso("GET", "/api/v1/loans", soloPrestador),
                new Caso("GET", "/api/v1/loans/" + id, soloPrestador),
                new Caso("POST", "/api/v1/prestamos", soloPrestador),
                new Caso("POST", "/api/v1/loans", soloPrestador),
                new Caso("POST", "/api/v1/prestamos/" + id + "/devoluciones", soloPrestador),
                new Caso("POST", "/api/v1/prestamos/" + id + "/incidencias", soloPrestador),
                new Caso("GET", "/api/v1/devoluciones", soloPrestador),
                new Caso("GET", "/api/v1/devoluciones/" + id, soloPrestador),
                new Caso("GET", "/api/v1/incidencias", Set.of(Rol.ADMIN, Rol.PRESTADOR)),
                new Caso("PATCH", "/api/v1/incidencias/" + id + "/resolucion", soloAdmin),
                new Caso("GET", "/api/v1/auth/me", Set.of(Rol.ADMIN, Rol.PRESTADOR)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matriz")
    @DisplayName("Matriz de roles: 401 sin token, 403 con rol no autorizado, acceso con rol autorizado")
    void matrizDeRoles(Caso caso) throws Exception {
        int sinToken = enviar(json(caso.metodo(), caso.ruta(), "{}", null)).statusCode();
        if (caso.permitidos().isEmpty()) {
            assertThat(sinToken).as("público sin token").isNotIn(401, 403);
        } else {
            assertThat(sinToken).as("sin token").isEqualTo(401);
        }

        for (Rol rol : new Rol[] {Rol.ADMIN, Rol.PRESTADOR}) {
            int estado = enviar(json(caso.metodo(), caso.ruta(), "{}", tokenMatriz(rol))).statusCode();
            if (caso.permitidos().isEmpty() || caso.permitidos().contains(rol)) {
                // Mientras el caso de uso no esté implementado la ruta responde 404/405; nunca 401/403.
                assertThat(estado).as(rol + " autorizado").isNotIn(401, 403);
            } else {
                assertThat(estado).as(rol + " no autorizado").isEqualTo(403);
            }
        }
    }

    // ------------------------------------------------------------- auxiliares

    record Cuenta(String identificacion, String correo, Rol rol, String contrasena) {
    }

    private void asegurar(Cuenta cuenta, boolean activa) {
        Usuario usuario = usuarios.findByIdentificacion(cuenta.identificacion())
                .orElseGet(() -> usuarios.save(Usuario.conAcceso(cuenta.identificacion(), "Cuenta", "Pruebas",
                        cuenta.correo(), codificador.encode(cuenta.contrasena()), cuenta.rol())));
        if (usuario.isActivo() != activa) {
            if (activa) {
                usuario.activar();
            } else {
                usuario.desactivar();
            }
            usuarios.save(usuario);
        }
    }

    private String tokenMatriz(Rol rol) throws Exception {
        if (!TOKENS_MATRIZ.containsKey(rol)) {
            Cuenta cuenta = rol == Rol.ADMIN ? ADMIN : PRESTADOR;
            TOKENS_MATRIZ.put(rol, tokenDe(cuenta));
        }
        return TOKENS_MATRIZ.get(rol);
    }

    private String tokenDe(Cuenta cuenta) throws Exception {
        HttpResponse<String> respuesta = login(cuenta.correo(), cuenta.contrasena());
        assertThat(respuesta.statusCode()).as("login de " + cuenta.identificacion()).isEqualTo(200);
        return extraerToken(respuesta);
    }

    private static String firmar(JwtEncoder codificador, Usuario usuario, Instant emitido, Instant expira,
            String emisor) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(usuario.getId().toString())
                .issuedAt(emitido)
                .expiresAt(expira)
                .claim(EmisorTokens.CLAIM_ROL, usuario.getRol().name())
                .claim(EmisorTokens.CLAIM_VERSION, usuario.getVersionCredenciales())
                .build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private void assertSesionInvalida(String token) throws Exception {
        HttpResponse<String> respuesta = enviar(peticion("GET", "/api/v1/auth/me", token));
        assertThat(respuesta.statusCode()).isEqualTo(401);
        assertThat(respuesta.body()).contains("\"codigo\":\"SESION_INVALIDA\"");
    }

    private HttpResponse<String> login(String identificador, String contrasena) throws Exception {
        String cuerpo = "{\"identificador\":\"" + identificador + "\",\"contrasena\":\"" + contrasena + "\"}";
        return enviar(json("POST", "/api/v1/auth/login", cuerpo, null));
    }

    private HttpRequest peticion(String metodo, String ruta, String token) {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .method(metodo, HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            constructor.header("Authorization", "Bearer " + token);
        }
        return constructor.build();
    }

    private HttpRequest json(String metodo, String ruta, String cuerpo, String token) {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo));
        if (token != null) {
            constructor.header("Authorization", "Bearer " + token);
        }
        return constructor.build();
    }

    private HttpResponse<String> enviar(HttpRequest solicitud) throws IOException, InterruptedException {
        return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());
    }

    private static String extraerToken(HttpResponse<String> respuesta) {
        Matcher m = TOKEN.matcher(respuesta.body());
        assertThat(m.find()).as("respuesta con tokenAcceso").isTrue();
        return m.group(1);
    }

    private static String detalle(HttpResponse<String> respuesta) {
        Matcher m = DETALLE.matcher(respuesta.body());
        return m.find() ? m.group(1) : "";
    }
}
