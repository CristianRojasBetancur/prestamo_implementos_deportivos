package co.edu.itm.prestamosdeportivos.persistencia;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrestamoRepositorio extends JpaRepository<Prestamo, UUID> {

    Optional<Prestamo> findByCodigo(String codigo);

    long countByUsuarioIdAndEstadoNot(UUID usuarioId, EstadoPrestamo estado);

    @Query("select count(distinct p.id) from Prestamo p join p.detalles d where p.usuario.id = :usuarioId "
            + "and p.estado <> :finalizado and p.fechaLimiteDevolucion < :ahora "
            + "and d.estado = co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo.PENDIENTE_DEVOLUCION")
    long contarPrestamosVencidosPendientes(@Param("usuarioId") UUID usuarioId,
            @Param("finalizado") EstadoPrestamo finalizado, @Param("ahora") Instant ahora);

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
