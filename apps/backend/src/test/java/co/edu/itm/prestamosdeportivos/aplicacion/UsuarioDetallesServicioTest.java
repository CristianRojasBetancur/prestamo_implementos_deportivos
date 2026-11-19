package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.itm.prestamosdeportivos.dominio.RolUsuario;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class UsuarioDetallesServicioTest {

    @Mock UsuarioRepositorio usuarios;

    @Test
    @DisplayName("BE-01 §5: usuario activo con rol PRESTADOR puede autenticarse")
    void prestadorActivoHabilitado() {
        Usuario usuario = usuario(RolUsuario.PRESTADOR, true);
        when(usuarios.findFirstByIdentificacionOrCorreoIgnoreCase("ana@itm.edu.co", "ana@itm.edu.co"))
                .thenReturn(Optional.of(usuario));

        var detalles = new UsuarioDetallesServicio(usuarios).loadUserByUsername("ana@itm.edu.co");

        assertThat(detalles.isEnabled()).isTrue();
        assertThat(detalles.getAuthorities()).extracting(Object::toString).contains("ROLE_PRESTADOR");
    }

    @Test
    @DisplayName("BE-01 §5: prestador inactivo no puede autenticarse")
    void prestadorInactivoDeshabilitado() {
        Usuario usuario = usuario(RolUsuario.PRESTADOR, false);
        when(usuarios.findFirstByIdentificacionOrCorreoIgnoreCase("ana@itm.edu.co", "ana@itm.edu.co"))
                .thenReturn(Optional.of(usuario));

        var detalles = new UsuarioDetallesServicio(usuarios).loadUserByUsername("ana@itm.edu.co");

        assertThat(detalles.isEnabled()).isFalse();
    }

    private Usuario usuario(RolUsuario rol, boolean activo) {
        return new Usuario("1001", "Ana", "Pérez", "ana@itm.edu.co", "{bcrypt}hash",
                rol, activo, Instant.parse("2026-11-18T16:00:00Z"));
    }
}
