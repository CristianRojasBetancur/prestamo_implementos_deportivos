package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.EmisorTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-01 (iniciar sesión), CU-02 (sesión actual) y cierre de sesión.
 *
 * <p>Para no revelar si una cuenta existe, todo fallo de inicio de sesión
 * (usuario inexistente, contraseña incorrecta, cuenta inactiva o rol USER)
 * produce el mismo error {@code CREDENCIALES_INVALIDAS}, y siempre se ejecuta una
 * verificación BCrypt (contra un hash señuelo si el usuario no existe), de modo
 * que el tiempo de respuesta tampoco delate el caso.
 */
@Service
public class AutenticacionServicio {

    private static final Logger LOG = LoggerFactory.getLogger(AutenticacionServicio.class);

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final EmisorTokens emisorTokens;
    private final String hashSenuelo;

    public AutenticacionServicio(UsuarioRepositorio usuarios, PasswordEncoder codificador, EmisorTokens emisorTokens) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.emisorTokens = emisorTokens;
        this.hashSenuelo = codificador.encode("senuelo-" + UUID.randomUUID());
    }

    @Transactional(readOnly = true)
    public SesionEmitida iniciarSesion(String identificador, String contrasena) {
        Optional<Usuario> candidato = buscar(identificador);
        String hash = candidato.map(Usuario::getContrasenaHash).orElse(hashSenuelo);
        boolean contrasenaValida = codificador.matches(contrasena, hash != null ? hash : hashSenuelo);

        if (candidato.isEmpty() || !contrasenaValida || !candidato.get().puedeIniciarSesion()) {
            // No se registra el identificador ni la contraseña; el MDC aporta la correlación.
            LOG.info("Inicio de sesión rechazado");
            throw new ExcepcionDominio(CodigoError.CREDENCIALES_INVALIDAS);
        }
        Usuario usuario = candidato.get();
        LOG.info("Inicio de sesión exitoso de usuario {} con rol {}", usuario.getId(), usuario.getRol());
        return new SesionEmitida(usuario, emisorTokens.emitir(usuario));
    }

    @Transactional(readOnly = true)
    public Usuario usuarioActual(UUID idUsuario) {
        return usuarios.findById(idUsuario)
                .filter(Usuario::puedeIniciarSesion)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.SESION_INVALIDA));
    }

    /** Revoca todas las sesiones del usuario (todos sus tokens dejan de ser válidos). */
    @Transactional
    public void cerrarSesion(UUID idUsuario) {
        usuarios.findById(idUsuario).ifPresent(usuario -> {
            usuario.revocarSesiones();
            LOG.info("Sesiones revocadas para el usuario {}", usuario.getId());
        });
    }

    private Optional<Usuario> buscar(String identificador) {
        String valor = identificador == null ? "" : identificador.trim();
        return valor.contains("@")
                ? usuarios.buscarPorCorreo(valor.toLowerCase(Locale.ROOT))
                : usuarios.findByIdentificacion(valor);
    }
}
