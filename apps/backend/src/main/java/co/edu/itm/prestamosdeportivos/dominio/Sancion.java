package co.edu.itm.prestamosdeportivos.dominio;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Suspensión por devolución tardía sin justificación (RN-03). */
@Entity
@Table(name = "sancion")
public class Sancion extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "minutos_retraso", nullable = false)
    private long minutosRetraso;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;

    @Column(name = "justificacion", length = 1000)
    private String justificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSancion estado = EstadoSancion.ACTIVA;

    protected Sancion() {
        // Requerido por JPA.
    }

    public Sancion(Usuario usuario, Prestamo prestamo, Devolucion devolucion, String motivo, long minutosRetraso,
            Instant fechaInicio, Instant fechaFin, String justificacion) {
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.prestamo = Objects.requireNonNull(prestamo, "prestamo");
        this.devolucion = Objects.requireNonNull(devolucion, "devolucion");
        this.motivo = Objects.requireNonNull(motivo, "motivo");
        this.minutosRetraso = minutosRetraso;
        this.fechaInicio = Objects.requireNonNull(fechaInicio, "fechaInicio");
        this.fechaFin = Objects.requireNonNull(fechaFin, "fechaFin");
        this.justificacion = justificacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public Devolucion getDevolucion() {
        return devolucion;
    }

    public String getMotivo() {
        return motivo;
    }

    public long getMinutosRetraso() {
        return minutosRetraso;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public EstadoSancion getEstado() {
        return estado;
    }
}
