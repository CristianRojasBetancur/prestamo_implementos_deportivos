package co.edu.itm.prestamosdeportivos.persistencia;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoSancion;
import co.edu.itm.prestamosdeportivos.dominio.Sancion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SancionRepositorio extends JpaRepository<Sancion, UUID> {

    Optional<Sancion> findFirstByUsuarioIdAndEstadoAndFechaFinAfterOrderByFechaFinDesc(UUID usuarioId,
            EstadoSancion estado, Instant ahora);

    /** Sanción activa de mayor fecha fin que sigue vigente en {@code ahora} (BE-01 §7). */
    default Optional<Sancion> buscarVigente(UUID usuarioId, Instant ahora) {
        return findFirstByUsuarioIdAndEstadoAndFechaFinAfterOrderByFechaFinDesc(usuarioId, EstadoSancion.ACTIVA, ahora);
    }

    List<Sancion> findByUsuarioIdOrderByFechaInicioDesc(UUID usuarioId);

    Optional<Sancion> findByDevolucionId(UUID devolucionId);
}
