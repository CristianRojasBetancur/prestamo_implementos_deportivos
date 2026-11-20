package co.edu.itm.prestamosdeportivos.soporte;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Datos de prueba persistentes y aislados por escenario.
 *
 * <p>Esta clase no se registra en el perfil local ni en producción. Los tests
 * la importan explícitamente y todos los identificadores generados llevan un
 * sufijo único para que una suite no dependa del orden de otra.</p>
 */
public final class FabricaFixtures {

    private final UsuarioRepositorio usuarios;
    private final CategoriaRepositorio categorias;
    private final ImplementoDeportivoRepositorio implementos;
    private final PasswordEncoder contrasenas;
    private final Clock reloj;
    private final AtomicInteger secuencia = new AtomicInteger();

    public FabricaFixtures(UsuarioRepositorio usuarios, CategoriaRepositorio categorias,
            ImplementoDeportivoRepositorio implementos, PasswordEncoder contrasenas, Clock reloj) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.implementos = implementos;
        this.contrasenas = contrasenas;
        this.reloj = reloj;
    }

    public Escenario escenario() {
        String sufijo = sufijo();
        Usuario user = usuario("USER-" + sufijo, "Usuario", "Fixture", Rol.USER, null, null);
        Cuenta admin = cuenta("ADMIN-" + sufijo, "admin." + sufijo + "@pruebas.invalid", Rol.ADMIN);
        Cuenta prestador = cuenta("PREST-" + sufijo, "prestador." + sufijo + "@pruebas.invalid", Rol.PRESTADOR);
        Categoria categoria = categoria("Balones " + sufijo);
        List<ImplementoDeportivo> unidades = List.of(
                unidad("BAL-" + sufijo + "-1", categoria, EstadoImplemento.DISPONIBLE),
                unidad("BAL-" + sufijo + "-2", categoria, EstadoImplemento.DISPONIBLE),
                unidad("BAL-" + sufijo + "-3", categoria, EstadoImplemento.DISPONIBLE));
        return new Escenario(user, admin, prestador, categoria, unidades);
    }

    public Cuenta cuenta(String identificacion, String correo, Rol rol) {
        String contrasena = "Fixture#2026-" + secuencia.incrementAndGet();
        Usuario usuario = usuario(identificacion, rol == Rol.ADMIN ? "Administrador" : "Prestador",
                "Fixture", rol, correo, contrasena);
        return new Cuenta(usuario, contrasena);
    }

    public Usuario usuario(String identificacion, String nombres, String apellidos, Rol rol,
            String correo, String contrasenaPlana) {
        String hash = contrasenaPlana == null ? null : contrasenas.encode(contrasenaPlana);
        Usuario usuario = new Usuario(identificacion, nombres, apellidos, correo, hash, rol, true, reloj.instant());
        return usuarios.saveAndFlush(usuario);
    }

    public Categoria categoria(String nombre) {
        return categorias.saveAndFlush(new Categoria(nombre, "Fixture reproducible"));
    }

    public ImplementoDeportivo unidad(String codigo, Categoria categoria, EstadoImplemento estado) {
        return implementos.saveAndFlush(new ImplementoDeportivo(codigo, "Unidad fixture",
                "Datos exclusivos de pruebas", categoria, estado));
    }

    private String sufijo() {
        return String.format("%04d-%s", secuencia.incrementAndGet(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 8));
    }

    public record Cuenta(Usuario usuario, String contrasena) {
        public UUID id() {
            return usuario.getId();
        }

        public String identificador() {
            return usuario.getIdentificacion();
        }

        public String correo() {
            return usuario.getCorreo();
        }
    }

    public record Escenario(Usuario usuario, Cuenta admin, Cuenta prestador, Categoria categoria,
            List<ImplementoDeportivo> unidades) {
    }
}
