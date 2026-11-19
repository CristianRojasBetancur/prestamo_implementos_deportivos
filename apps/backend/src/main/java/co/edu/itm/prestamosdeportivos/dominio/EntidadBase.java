package co.edu.itm.prestamosdeportivos.dominio;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Identidad UUID y marcas de auditoría comunes a todas las tablas del MVP.
 * Las marcas de auditoría son técnicas; las fechas de negocio (préstamo,
 * límite, devolución, sanción) se calculan con el {@code Clock} inyectado.
 */
@MappedSuperclass
public abstract class EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public UUID getId() {
        return id;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    protected void inicializarAuditoria(Instant ahora) {
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    protected void marcarActualizado(Instant ahora) {
        this.actualizadoEn = ahora;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || Hibernate.getClass(this) != Hibernate.getClass(otro)) {
            return false;
        }
        return getId() != null && getId().equals(((EntidadBase) otro).getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
