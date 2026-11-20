package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ImplementoDeportivoRepositorio extends JpaRepository<ImplementoDeportivo, UUID>,
        JpaSpecificationExecutor<ImplementoDeportivo> {

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<ImplementoDeportivo> findAll(Specification<ImplementoDeportivo> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Optional<ImplementoDeportivo> findById(UUID id);

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

    /**
     * Consulta en lote el estado y las condiciones que excluyen una unidad del
     * préstamo. Se usa durante la segunda validación dentro de la transacción.
     */
    @Query(value = """
            SELECT i.id AS id,
                   i.codigo_inventario AS codigoInventario,
                   i.estado AS estado,
                   EXISTS (
                       SELECT 1 FROM detalle_prestamo d
                        WHERE d.implemento_id = i.id
                          AND d.estado = 'PENDIENTE_DEVOLUCION'
                   ) AS pendienteDevolucion,
                   EXISTS (
                       SELECT 1 FROM incidencia inc
                        WHERE inc.implemento_id = i.id
                          AND inc.estado = 'REGISTRADA'
                   ) AS incidenciaRegistrada
              FROM implemento_deportivo i
             WHERE i.id IN (:ids)
             ORDER BY i.id
            """, nativeQuery = true)
    List<DisponibilidadUnidad> verificarDisponibilidad(@Param("ids") Collection<UUID> ids);
}
