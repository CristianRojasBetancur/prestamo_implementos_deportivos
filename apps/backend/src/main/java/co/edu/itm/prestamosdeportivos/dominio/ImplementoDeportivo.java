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
import jakarta.persistence.Version;

/**
 * Unidad física del inventario (una fila por unidad). El comportamiento por
 * estado (patrón State) se incorpora con los casos de uso de inventario y
 * préstamo. {@code version} protege la edición concurrente (BE-01 §11).
 */
@Entity
@Table(name = "implemento_deportivo")
public class ImplementoDeportivo extends EntidadBase {

    @Column(name = "codigo_inventario", nullable = false, length = 30)
    private String codigoInventario;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoImplemento estado;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ImplementoDeportivo() {
        // Requerido por JPA.
    }

    public ImplementoDeportivo(String codigoInventario, String nombre, String descripcion, Categoria categoria,
            EstadoImplemento estado) {
        this.codigoInventario = Objects.requireNonNull(codigoInventario, "codigoInventario");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.descripcion = descripcion;
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public String getCodigoInventario() {
        return codigoInventario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public EstadoImplemento getEstado() {
        return estado;
    }

    public Long getVersion() {
        return version;
    }
}
