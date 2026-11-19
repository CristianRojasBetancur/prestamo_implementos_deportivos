package co.edu.itm.prestamosdeportivos.dominio;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Clasificación de los implementos; nombre único sin distinguir mayúsculas. */
@Entity
@Table(name = "categoria")
public class Categoria extends EntidadBase {

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    protected Categoria() {
        // Requerido por JPA.
    }

    public Categoria(String nombre, String descripcion) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActiva() {
        return activa;
    }
}
