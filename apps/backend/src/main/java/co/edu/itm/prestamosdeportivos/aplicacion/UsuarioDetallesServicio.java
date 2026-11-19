package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.Locale;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.edu.itm.prestamosdeportivos.dominio.RolUsuario;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;

/** Carga credenciales para el CU-01 definido en BE-01; no expone una ruta de login. */
@Service
public class UsuarioDetallesServicio implements UserDetailsService {

    private final UsuarioRepositorio usuarios;

    public UsuarioDetallesServicio(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        String valor = identificador.trim();
        Usuario usuario = usuarios.findFirstByIdentificacionOrCorreoIgnoreCase(valor,
                        valor.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));

        boolean puedeAutenticarse = usuario.isActivo() && usuario.getRol() != RolUsuario.USER;
        return User.withUsername(usuario.getIdentificacion())
                .password(usuario.getContrasenaHash() == null ? "" : usuario.getContrasenaHash())
                .roles(usuario.getRol().name())
                .disabled(!puedeAutenticarse)
                .build();
    }
}
