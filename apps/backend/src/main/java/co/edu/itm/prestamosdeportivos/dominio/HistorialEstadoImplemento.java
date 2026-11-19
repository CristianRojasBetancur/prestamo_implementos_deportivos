package co.edu.itm.prestamosdeportivos.dominio;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Auditoría inmutable de cada cambio de estado de una unidad física. */
@Entity
@Table(name = "historial_estado_implemento")
public class HistorialEstadoImplemento extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "implemento_id", nullable = false, updatable = false)
    private ImplementoDeportivo implemento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20, updatable = false)
    private EstadoImplemento estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20, updatable = false)
    private EstadoImplemento estadoNuevo;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 20, updatable = false)
    private EventoEstadoImplemento evento;

    @Column(name = "motivo", nullable = false, length = 500, updatable = false)
    private String motivo;

    protected HistorialEstadoImplemento() {
    }

    public HistorialEstadoImplemento(ImplementoDeportivo implemento, EstadoImplemento estadoAnterior,
            EstadoImplemento estadoNuevo, EventoEstadoImplemento evento, String motivo) {
        this.implemento = Objects.requireNonNull(implemento, "implemento");
        this.estadoAnterior = Objects.requireNonNull(estadoAnterior, "estadoAnterior");
        this.estadoNuevo = Objects.requireNonNull(estadoNuevo, "estadoNuevo");
        this.evento = Objects.requireNonNull(evento, "evento");
        this.motivo = Objects.requireNonNull(motivo, "motivo");
    }

    public EstadoImplemento getEstadoAnterior() { return estadoAnterior; }
    public EstadoImplemento getEstadoNuevo() { return estadoNuevo; }
    public EventoEstadoImplemento getEvento() { return evento; }
    public String getMotivo() { return motivo; }
}
