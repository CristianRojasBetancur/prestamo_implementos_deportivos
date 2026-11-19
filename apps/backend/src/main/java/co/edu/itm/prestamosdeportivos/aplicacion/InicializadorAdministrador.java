package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.Locale;

import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el primer administrador al arrancar (BE-01 §11), solo si se definen
 * {@code APP_ADMIN_CORREO} y {@code APP_ADMIN_CONTRASENA} y aún no existe ningún
 * usuario con ese correo. No hay credenciales en el código ni en el repositorio:
 * sin esas variables no se crea nada.
 */
@Component
public class InicializadorAdministrador implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InicializadorAdministrador.class);
    private static final int LONGITUD_MINIMA_CONTRASENA = 12;

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder codificador;
    private final String correo;
    private final String contrasena;
    private final String identificacion;

    public InicializadorAdministrador(UsuarioRepositorio usuarios, PasswordEncoder codificador,
            @Value("${app.admin-inicial.correo:}") String correo,
            @Value("${app.admin-inicial.contrasena:}") String contrasena,
            @Value("${app.admin-inicial.identificacion:ADMIN-0001}") String identificacion) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.correo = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        this.contrasena = contrasena == null ? "" : contrasena;
        this.identificacion = identificacion;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (correo.isEmpty() || contrasena.isEmpty()) {
            return;
        }
        if (contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            LOG.warn("APP_ADMIN_CONTRASENA debe tener al menos {} caracteres; no se creó el administrador inicial.",
                    LONGITUD_MINIMA_CONTRASENA);
            return;
        }
        if (usuarios.existeCorreo(correo) || usuarios.existsByIdentificacion(identificacion)) {
            return;
        }
        usuarios.save(Usuario.conAcceso(identificacion, "Administrador", "Inicial", correo,
                codificador.encode(contrasena), Rol.ADMIN));
        LOG.info("Administrador inicial creado con identificación {}", identificacion);
    }
}
