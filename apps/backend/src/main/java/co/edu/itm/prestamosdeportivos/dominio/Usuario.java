package co.edu.itm.prestamosdeportivos.dominio;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Usuario solicitante, prestador o administrador del MVP. */
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
    @Column(name = "suspendido_hasta")
    private Instant suspendidoHasta;
    @Column(name = "version_credenciales", nullable = false)
    private int versionCredenciales;

    protected Usuario() {
    }

    public Usuario(String identificacion, String nombres, String apellidos, String correo,
            String contrasenaHash, Rol rol, boolean activo, Instant ahora) {
        this.identificacion = Objects.requireNonNull(identificacion, "identificacion");
        this.nombres = Objects.requireNonNull(nombres, "nombres");
        this.apellidos = Objects.requireNonNull(apellidos, "apellidos");
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = Objects.requireNonNull(rol, "rol");
        this.activo = activo;
        inicializarAuditoria(ahora);
    }

    public static Usuario solicitante(String identificacion, String nombres, String apellidos) {
        return new Usuario(identificacion, nombres, apellidos, null, null, Rol.USER, true, Instant.now());
    }

    public static Usuario conAcceso(String identificacion, String nombres, String apellidos, String correo,
            String contrasenaHash, Rol rol) {
        if (rol == Rol.USER) {
            throw new IllegalArgumentException("Un usuario con rol USER no tiene acceso al sistema.");
        }
        return new Usuario(identificacion, nombres, apellidos, Objects.requireNonNull(correo, "correo"),
                Objects.requireNonNull(contrasenaHash, "contrasenaHash"), rol, true, Instant.now());
    }

    public boolean puedeIniciarSesion() {
        return activo && rol != Rol.USER && contrasenaHash != null;
    }

    public void actualizarPrestador(String nombres, String apellidos, String identificacion, String correo,
            Instant ahora) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.identificacion = identificacion;
        this.correo = correo;
        marcarActualizado(ahora);
    }

    public void cambiarEstado(boolean activo, Instant ahora) {
        if (this.activo && !activo) {
            versionCredenciales++;
        }
        this.activo = activo;
        marcarActualizado(ahora);
    }

    public void desactivar() {
        cambiarEstado(false, Instant.now());
    }

    public void activar() {
        cambiarEstado(true, Instant.now());
    }

    public void revocarSesiones() {
        versionCredenciales++;
    }

    public String getIdentificacion() { return identificacion; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public String getContrasenaHash() { return contrasenaHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public Instant getSuspendidoHasta() { return suspendidoHasta; }
    public int getVersionCredenciales() { return versionCredenciales; }
}
