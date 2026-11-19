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
import org.hibernate.annotations.Generated;

/**
 * Novedad registrada sobre una unidad de un préstamo. {@code prestamo} e
 * {@code implemento} replican los del detalle para facilitar las consultas; una
 * FK compuesta en PostgreSQL impide que sean incoherentes con el detalle.
 */
@Entity
@Table(name = "incidencia")
public class Incidencia extends EntidadBase {

    @Generated
    @Column(name = "codigo", nullable = false, insertable = false, updatable = false, length = 12)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_prestamo_id", nullable = false)
    private DetallePrestamo detallePrestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "implemento_id", nullable = false)
    private ImplementoDeportivo implemento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Usuario prestador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoIncidencia tipo;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoIncidencia estado = EstadoIncidencia.REGISTRADA;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    protected Incidencia() {
        // Requerido por JPA.
    }

    public Incidencia(DetallePrestamo detallePrestamo, Usuario prestador, TipoIncidencia tipo, String descripcion,
            Instant fechaRegistro) {
        this.detallePrestamo = Objects.requireNonNull(detallePrestamo, "detallePrestamo");
        this.prestamo = detallePrestamo.getPrestamo();
        this.implemento = detallePrestamo.getImplemento();
        this.prestador = Objects.requireNonNull(prestador, "prestador");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.descripcion = Objects.requireNonNull(descripcion, "descripcion");
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "fechaRegistro");
    }

    public String getCodigo() {
        return codigo;
    }

    public DetallePrestamo getDetallePrestamo() {
        return detallePrestamo;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public ImplementoDeportivo getImplemento() {
        return implemento;
    }

    public Usuario getPrestador() {
        return prestador;
    }

    public TipoIncidencia getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoIncidencia getEstado() {
        return estado;
    }

    public Instant getFechaRegistro() {
        return fechaRegistro;
    }

    public Instant getFechaResolucion() {
        return fechaResolucion;
    }
}
