package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.DetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePrestamoRepositorio extends JpaRepository<DetallePrestamo, UUID> {

    interface ConteoPorPrestamo {
        UUID getPrestamoId();
        long getCantidad();
        long getPendientes();
    }

    /** RN-01: unidades del usuario que siguen pendientes en cualquiera de sus préstamos. */
    long countByPrestamoUsuarioIdAndEstado(UUID usuarioId, EstadoDetallePrestamo estado);

    default long contarUnidadesPendientesDeUsuario(UUID usuarioId) {
        return countByPrestamoUsuarioIdAndEstado(usuarioId, EstadoDetallePrestamo.PENDIENTE_DEVOLUCION);
    }

    /** RN-06: ¿la unidad ya está pendiente en algún préstamo? */
    boolean existsByImplementoIdAndEstado(UUID implementoId, EstadoDetallePrestamo estado);

    List<DetallePrestamo> findByPrestamoIdAndEstado(UUID prestamoId, EstadoDetallePrestamo estado);

    @org.springframework.data.jpa.repository.Query("select d.prestamo.id as prestamoId, count(d.id) as cantidad, "
            + "sum(case when d.estado = co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo.PENDIENTE_DEVOLUCION "
            + "then 1 else 0 end) as pendientes from DetallePrestamo d "
            + "where d.prestamo.id in :ids group by d.prestamo.id")
    List<ConteoPorPrestamo> contarPorPrestamos(@org.springframework.data.repository.query.Param("ids") List<UUID> ids);
}
