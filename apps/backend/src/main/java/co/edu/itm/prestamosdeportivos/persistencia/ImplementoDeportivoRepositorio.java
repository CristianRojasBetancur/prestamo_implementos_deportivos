package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImplementoDeportivoRepositorio extends JpaRepository<ImplementoDeportivo, UUID> {

    Optional<ImplementoDeportivo> findByCodigoInventario(String codigoInventario);

    boolean existsByCodigoInventario(String codigoInventario);

    long countByEstado(EstadoImplemento estado);

    /**
     * Bloquea las unidades ({@code SELECT ... FOR UPDATE}) en orden de UUID para
     * evitar deadlocks entre préstamos concurrentes (BE-01 §11). Debe llamarse
     * dentro de una transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ImplementoDeportivo i where i.id in :ids order by i.id")
    List<ImplementoDeportivo> bloquearPorIds(@Param("ids") Collection<UUID> ids);
}
