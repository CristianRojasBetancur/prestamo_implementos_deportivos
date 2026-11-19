package co.edu.itm.prestamosdeportivos.dominio;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Unidad recibida dentro de una devolución, con su condición e incidencia asociada. */
@Entity
@Table(name = "detalle_devolucion")
public class DetalleDevolucion extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_prestamo_id", nullable = false, unique = true)
    private DetallePrestamo detallePrestamo;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion", nullable = false, length = 20)
    private CondicionDevolucion condicion;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incidencia_id", unique = true)
    private Incidencia incidencia;

    protected DetalleDevolucion() {
        // Requerido por JPA.
    }

    DetalleDevolucion(Devolucion devolucion, DetallePrestamo detallePrestamo, CondicionDevolucion condicion,
            String observaciones, Incidencia incidencia) {
        this.devolucion = Objects.requireNonNull(devolucion, "devolucion");
        this.detallePrestamo = Objects.requireNonNull(detallePrestamo, "detallePrestamo");
        this.condicion = Objects.requireNonNull(condicion, "condicion");
        this.observaciones = observaciones;
        this.incidencia = incidencia;
    }

    public Devolucion getDevolucion() {
        return devolucion;
    }

    public DetallePrestamo getDetallePrestamo() {
        return detallePrestamo;
    }

    public CondicionDevolucion getCondicion() {
        return condicion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public Incidencia getIncidencia() {
        return incidencia;
    }
}
