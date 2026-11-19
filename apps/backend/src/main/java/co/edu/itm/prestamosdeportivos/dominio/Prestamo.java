package co.edu.itm.prestamosdeportivos.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;

/**
 * Préstamo registrado por un prestador para un usuario solicitante. El
 * {@code codigo} legible (PRE-000001) lo genera PostgreSQL al insertar.
 */
@Entity
@Table(name = "prestamo")
public class Prestamo extends EntidadBase {

    @Generated
    @Column(name = "codigo", nullable = false, insertable = false, updatable = false, length = 12)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Usuario prestador;

    @Column(name = "fecha_prestamo", nullable = false)
    private Instant fechaPrestamo;

    @Column(name = "fecha_limite_devolucion", nullable = false)
    private Instant fechaLimiteDevolucion;

    @Column(name = "fecha_finalizacion")
    private Instant fechaFinalizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPrestamo estado = EstadoPrestamo.VIGENTE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL)
    private List<DetallePrestamo> detalles = new ArrayList<>();

    protected Prestamo() {
        // Requerido por JPA.
    }

    public Prestamo(Usuario usuario, Usuario prestador, Instant fechaPrestamo, Instant fechaLimiteDevolucion) {
        this.usuario = Objects.requireNonNull(usuario, "usuario");
        this.prestador = Objects.requireNonNull(prestador, "prestador");
        this.fechaPrestamo = Objects.requireNonNull(fechaPrestamo, "fechaPrestamo");
        this.fechaLimiteDevolucion = Objects.requireNonNull(fechaLimiteDevolucion, "fechaLimiteDevolucion");
    }

    /** Asocia una unidad física al préstamo en estado PENDIENTE_DEVOLUCION. */
    public DetallePrestamo agregarDetalle(ImplementoDeportivo implemento) {
        DetallePrestamo detalle = new DetallePrestamo(this, implemento);
        detalles.add(detalle);
        return detalle;
    }

    public String getCodigo() {
        return codigo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Usuario getPrestador() {
        return prestador;
    }

    public Instant getFechaPrestamo() {
        return fechaPrestamo;
    }

    public Instant getFechaLimiteDevolucion() {
        return fechaLimiteDevolucion;
    }

    public Instant getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }

    public Long getVersion() {
        return version;
    }

    public List<DetallePrestamo> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    /** Evalúa el estado global después de resolver una o más unidades. */
    public void actualizarEstadoTrasDevolucion(Instant ahora) {
        Objects.requireNonNull(ahora, "ahora");
        if (estado == EstadoPrestamo.FINALIZADO) {
            throw new ExcepcionDominio(CodigoError.PRESTAMO_FINALIZADO);
        }
        boolean quedanPendientes = detalles.stream()
                .anyMatch(detalle -> detalle.getEstado() == EstadoDetallePrestamo.PENDIENTE_DEVOLUCION);
        if (!quedanPendientes) {
            estado = EstadoPrestamo.FINALIZADO;
            fechaFinalizacion = ahora;
            return;
        }
        if (ahora.compareTo(fechaLimiteDevolucion) >= 0 || estado == EstadoPrestamo.VENCIDO) {
            estado = EstadoPrestamo.VENCIDO;
        } else if (estado == EstadoPrestamo.VIGENTE) {
            estado = EstadoPrestamo.DEVOLUCION_PARCIAL;
        }
    }
}
