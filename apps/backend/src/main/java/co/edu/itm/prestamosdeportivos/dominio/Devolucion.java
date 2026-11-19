package co.edu.itm.prestamosdeportivos.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
import org.hibernate.annotations.Generated;

/** Registro de una devolución (total o parcial) de unidades de un préstamo. */
@Entity
@Table(name = "devolucion")
public class Devolucion extends EntidadBase {

    @Generated
    @Column(name = "codigo", nullable = false, insertable = false, updatable = false, length = 12)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Usuario prestador;

    @Column(name = "fecha_devolucion", nullable = false)
    private Instant fechaDevolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcance", nullable = false, length = 10)
    private AlcanceDevolucion alcance;

    @Column(name = "retraso_justificado", nullable = false)
    private boolean retrasoJustificado;

    @Column(name = "justificacion", length = 1000)
    private String justificacion;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL)
    private List<DetalleDevolucion> detalles = new ArrayList<>();

    protected Devolucion() {
        // Requerido por JPA.
    }

    public Devolucion(Prestamo prestamo, Usuario prestador, Instant fechaDevolucion, AlcanceDevolucion alcance,
            boolean retrasoJustificado, String justificacion, String observaciones) {
        this.prestamo = Objects.requireNonNull(prestamo, "prestamo");
        this.prestador = Objects.requireNonNull(prestador, "prestador");
        this.fechaDevolucion = Objects.requireNonNull(fechaDevolucion, "fechaDevolucion");
        this.alcance = Objects.requireNonNull(alcance, "alcance");
        this.retrasoJustificado = retrasoJustificado;
        this.justificacion = justificacion;
        this.observaciones = observaciones;
    }

    /** Registra una unidad recibida. {@code incidencia} es obligatoria si la condición es CON_NOVEDAD. */
    public DetalleDevolucion agregarDetalle(DetallePrestamo detallePrestamo, CondicionDevolucion condicion,
            String observaciones, Incidencia incidencia) {
        DetalleDevolucion detalle = new DetalleDevolucion(this, detallePrestamo, condicion, observaciones, incidencia);
        detalles.add(detalle);
        return detalle;
    }

    public String getCodigo() {
        return codigo;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public Usuario getPrestador() {
        return prestador;
    }

    public Instant getFechaDevolucion() {
        return fechaDevolucion;
    }

    public AlcanceDevolucion getAlcance() {
        return alcance;
    }

    public boolean isRetrasoJustificado() {
        return retrasoJustificado;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public List<DetalleDevolucion> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }
}
