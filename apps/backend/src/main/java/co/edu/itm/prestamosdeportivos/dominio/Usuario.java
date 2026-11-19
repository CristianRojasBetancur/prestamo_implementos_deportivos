package co.edu.itm.prestamosdeportivos.dominio;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Única entidad de acceso del sistema (BE-01 D-04, D-05). Un prestador o un
 * administrador es un {@code Usuario} con el rol correspondiente; un
 * solicitante es un {@code Usuario} con rol {@link Rol#USER}, sin contraseña.
 */
@Entity
@Table(name = "usuario")
public class Usuario extends EntidadBase {

    @Column(name = "identificacion", nullable = false, length = 30)
    private String identificacion;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "correo", length = 254)
    private String correo;

    @Column(name = "contrasena_hash", length = 100)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /** Fin de la suspensión vigente (caché de las sanciones; ver modelo-datos.md). */
    @Column(name = "suspendido_hasta")
    private Instant suspendidoHasta;

    @Column(name = "version_credenciales", nullable = false)
    private int versionCredenciales;

    protected Usuario() {
        // Requerido por JPA.
    }

    private Usuario(String identificacion, String nombres, String apellidos, String correo,
            String contrasenaHash, Rol rol) {
        this.identificacion = Objects.requireNonNull(identificacion, "identificacion");
        this.nombres = Objects.requireNonNull(nombres, "nombres");
        this.apellidos = Objects.requireNonNull(apellidos, "apellidos");
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = Objects.requireNonNull(rol, "rol");
    }

    /** Solicitante (rol USER): no inicia sesión y no tiene contraseña. */
    public static Usuario solicitante(String identificacion, String nombres, String apellidos) {
        return new Usuario(identificacion, nombres, apellidos, null, null, Rol.USER);
    }

    /** Usuario con acceso (PRESTADOR o ADMIN): correo y hash de contraseña obligatorios. */
    public static Usuario conAcceso(String identificacion, String nombres, String apellidos, String correo,
            String contrasenaHash, Rol rol) {
        if (rol == Rol.USER) {
            throw new IllegalArgumentException("Un usuario con rol USER no tiene acceso al sistema.");
        }
        return new Usuario(identificacion, nombres, apellidos,
                Objects.requireNonNull(correo, "correo"),
                Objects.requireNonNull(contrasenaHash, "contrasenaHash"), rol);
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getSuspendidoHasta() {
        return suspendidoHasta;
    }

    public int getVersionCredenciales() {
        return versionCredenciales;
    }
}
