package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestador;
import co.edu.itm.prestamosdeportivos.dominio.RolUsuario;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.web.ActualizarPrestadorSolicitud;
import co.edu.itm.prestamosdeportivos.web.CambiarEstadoPrestadorSolicitud;
import co.edu.itm.prestamosdeportivos.web.RegistrarPrestadorSolicitud;
import co.edu.itm.prestamosdeportivos.web.error.ExcepcionApi;

@ExtendWith(MockitoExtension.class)
class PrestadorServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T16:00:00Z");

    @Mock UsuarioRepositorio usuarios;
    @Mock PasswordEncoder passwordEncoder;
    private PrestadorServicio servicio;

    @BeforeEach
    void configurar() {
        servicio = new PrestadorServicio(usuarios, passwordEncoder,
                Clock.fixed(AHORA, ZoneOffset.UTC), "America/Bogota");
    }

    @Test
    @DisplayName("CU-03 registra prestador activo, hash de contraseña y rol PRESTADOR")
    void registraPrestador() {
        when(usuarios.existsByIdentificacion("1045678901")).thenReturn(false);
        when(usuarios.existsByCorreoIgnoreCase("felipe@itm.edu.co")).thenReturn(false);
        when(passwordEncoder.encode("Temporal#2026")).thenReturn("$2a$10$hash");
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = servicio.registrar(new RegistrarPrestadorSolicitud("Felipe", "Gómez",
                "1045678901", "Felipe@ITM.edu.co", "Temporal#2026"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getRol()).isEqualTo(RolUsuario.PRESTADOR);
        assertThat(guardado.isActivo()).isTrue();
        assertThat(guardado.getCorreo()).isEqualTo("felipe@itm.edu.co");
        assertThat(guardado.getContrasenaHash()).isEqualTo("$2a$10$hash");
        assertThat(guardado.getContrasenaHash()).isNotEqualTo("Temporal#2026");
        assertThat(resultado.estado()).isEqualTo("ACTIVO");
        assertThat(resultado.actualizadoEn()).isEqualTo(resultado.creadoEn());
    }

    @Test
    @DisplayName("CU-03 informa la identificación duplicada como conflicto de unicidad")
    void rechazaIdentificacionDuplicada() {
        when(usuarios.existsByIdentificacion("1045678901")).thenReturn(true);
        when(usuarios.existsByCorreoIgnoreCase(anyString())).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(new RegistrarPrestadorSolicitud("Felipe", "Gómez",
                "1045678901", "felipe@itm.edu.co", "Temporal#2026")))
                .isInstanceOfSatisfying(ExcepcionApi.class, ex -> {
                    assertThat(ex.getEstadoHttp()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCodigo()).isEqualTo("CONFLICTO_UNICIDAD");
                    assertThat(ex.getErrores()).extracting("campo").contains("identificacion");
                });
    }

    @Test
    @DisplayName("CU-03 rechaza correos duplicados sin distinguir mayúsculas")
    void rechazaCorreoDuplicado() {
        when(usuarios.existsByIdentificacion("1045678901")).thenReturn(false);
        when(usuarios.existsByCorreoIgnoreCase("felipe@itm.edu.co")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(new RegistrarPrestadorSolicitud("Felipe", "Gómez",
                "1045678901", "Felipe@ITM.edu.co", "Temporal#2026")))
                .isInstanceOfSatisfying(ExcepcionApi.class, ex -> {
                    assertThat(ex.getEstadoHttp()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getErrores()).extracting("campo").contains("correo");
                });
    }

    @Test
    @DisplayName("CU-05 actualiza solo los campos permitidos y conserva rol y credenciales")
    void actualizaDatosPermitidos() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario("123", "Ana", "Pérez", "ana@itm.edu.co", "hash",
                RolUsuario.PRESTADOR, true, AHORA.minusSeconds(60));
        when(usuarios.findByIdAndRol(id, RolUsuario.PRESTADOR)).thenReturn(Optional.of(usuario));
        when(usuarios.existsByIdentificacionAndIdNot("123", id)).thenReturn(false);
        when(usuarios.existsByCorreoIgnoreCaseAndIdNot("ana@itm.edu.co", id)).thenReturn(false);
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var resultado = servicio.actualizar(id, new ActualizarPrestadorSolicitud("Ana María", null, null, null));

        assertThat(resultado.nombres()).isEqualTo("Ana María");
        assertThat(usuario.getRol()).isEqualTo(RolUsuario.PRESTADOR);
        assertThat(usuario.getContrasenaHash()).isEqualTo("hash");
        assertThat(usuario.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("CU-06 desactivar conserva el registro e incrementa versionCredenciales")
    void desactivaYConservaRegistro() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario("123", "Ana", "Pérez", "ana@itm.edu.co", "hash",
                RolUsuario.PRESTADOR, true, AHORA.minusSeconds(60));
        when(usuarios.findByIdAndRol(id, RolUsuario.PRESTADOR)).thenReturn(Optional.of(usuario));
        when(usuarios.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = servicio.cambiarEstado(id,
                new CambiarEstadoPrestadorSolicitud(EstadoPrestador.INACTIVO));

        assertThat(respuesta.estado()).isEqualTo("INACTIVO");
        assertThat(usuario.getVersionCredenciales()).isEqualTo(1);
        assertThat(usuario.isActivo()).isFalse();
        verify(usuarios).saveAndFlush(usuario);
    }
}
