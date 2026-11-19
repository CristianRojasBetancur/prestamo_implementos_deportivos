package co.edu.itm.prestamosdeportivos.persistencia;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestamoRepositorio extends JpaRepository<Prestamo, UUID> {

    Optional<Prestamo> findByCodigo(String codigo);

    long countByUsuarioIdAndEstadoNot(UUID usuarioId, EstadoPrestamo estado);

    /**
     * RN-05: ¿tiene el usuario préstamos no finalizados con la fecha límite
     * superada? Se evalúa por fecha, sin depender de que la tarea de vencimiento
     * ya haya cambiado el estado (BE-01 §7).
     */
    boolean existsByUsuarioIdAndEstadoNotAndFechaLimiteDevolucionBefore(UUID usuarioId, EstadoPrestamo estado,
            Instant ahora);

    default boolean tienePrestamosVencidos(UUID usuarioId, Instant ahora) {
        return existsByUsuarioIdAndEstadoNotAndFechaLimiteDevolucionBefore(usuarioId, EstadoPrestamo.FINALIZADO, ahora);
    }
}
