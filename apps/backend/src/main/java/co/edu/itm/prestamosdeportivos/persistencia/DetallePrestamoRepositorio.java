package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.DetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePrestamoRepositorio extends JpaRepository<DetallePrestamo, UUID> {

    /** RN-01: unidades del usuario que siguen pendientes en cualquiera de sus préstamos. */
    long countByPrestamoUsuarioIdAndEstado(UUID usuarioId, EstadoDetallePrestamo estado);

    default long contarUnidadesPendientesDeUsuario(UUID usuarioId) {
        return countByPrestamoUsuarioIdAndEstado(usuarioId, EstadoDetallePrestamo.PENDIENTE_DEVOLUCION);
    }

    /** RN-06: ¿la unidad ya está pendiente en algún préstamo? */
    boolean existsByImplementoIdAndEstado(UUID implementoId, EstadoDetallePrestamo estado);

    List<DetallePrestamo> findByPrestamoIdAndEstado(UUID prestamoId, EstadoDetallePrestamo estado);
}
