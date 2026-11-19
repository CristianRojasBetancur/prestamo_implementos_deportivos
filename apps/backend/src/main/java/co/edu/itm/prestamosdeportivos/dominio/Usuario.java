package co.edu.itm.prestamosdeportivos.dominio;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 40, unique = true)
    private String identificacion;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Column(length = 254)
    private String correo;

    @Column(name = "contrasena_hash", length = 100)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "version_credenciales", nullable = false)
    private long versionCredenciales;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Usuario() { }

    public Usuario(String identificacion, String nombres, String apellidos, String correo,
            String contrasenaHash, RolUsuario rol, boolean activo, Instant ahora) {
        this.identificacion = identificacion;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.activo = activo;
        this.versionCredenciales = 0;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public void actualizarPrestador(String nombres, String apellidos, String identificacion, String correo,
            Instant ahora) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.identificacion = identificacion;
        this.correo = correo;
        this.actualizadoEn = ahora;
    }

    public void cambiarEstado(boolean activo, Instant ahora) {
        if (this.activo && !activo) {
            this.versionCredenciales++;
        }
        this.activo = activo;
        this.actualizadoEn = ahora;
    }

    public UUID getId() { return id; }
    public String getIdentificacion() { return identificacion; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public String getContrasenaHash() { return contrasenaHash; }
    public RolUsuario getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public long getVersionCredenciales() { return versionCredenciales; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
