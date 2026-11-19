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

/**
 * Una unidad física dentro de un préstamo. PostgreSQL garantiza que una unidad
 * solo tenga un detalle en PENDIENTE_DEVOLUCION a la vez (RN-06) y que estado,
 * fecha de resolución y condición sean coherentes.
 */
@Entity
@Table(name = "detalle_prestamo")
public class DetallePrestamo extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "implemento_id", nullable = false)
    private ImplementoDeportivo implemento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoDetallePrestamo estado = EstadoDetallePrestamo.PENDIENTE_DEVOLUCION;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_recibida", length = 20)
    private CondicionDevolucion condicionRecibida;

    protected DetallePrestamo() {
        // Requerido por JPA.
    }

    DetallePrestamo(Prestamo prestamo, ImplementoDeportivo implemento) {
        this.prestamo = Objects.requireNonNull(prestamo, "prestamo");
        this.implemento = Objects.requireNonNull(implemento, "implemento");
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public ImplementoDeportivo getImplemento() {
        return implemento;
    }

    public EstadoDetallePrestamo getEstado() {
        return estado;
    }

    public Instant getFechaResolucion() {
        return fechaResolucion;
    }

    public CondicionDevolucion getCondicionRecibida() {
        return condicionRecibida;
    }
}
