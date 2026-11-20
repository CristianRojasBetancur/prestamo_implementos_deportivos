package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Incidencia;
import co.edu.itm.prestamosdeportivos.dominio.EstadoIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface IncidenciaRepositorio extends JpaRepository<Incidencia, UUID>, JpaSpecificationExecutor<Incidencia> {

    @Override
    @EntityGraph(attributePaths = {"prestamo", "prestamo.usuario", "implemento", "prestador", "resueltoPor"})
    Page<Incidencia> findAll(Specification<Incidencia> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"prestamo", "prestamo.usuario", "implemento", "prestador", "resueltoPor"})
    Optional<Incidencia> findById(UUID id);

    /** Serializa dos resoluciones administrativas concurrentes de la misma incidencia. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"prestamo", "prestamo.usuario", "implemento", "prestador", "resueltoPor"})
    @Query("select i from Incidencia i where i.id = :id")
    Optional<Incidencia> bloquearPorId(@Param("id") UUID id);

    Optional<Incidencia> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"implemento", "prestador", "resueltoPor"})
    List<Incidencia> findByPrestamoIdOrderByFechaRegistroDesc(UUID prestamoId);

    List<Incidencia> findByImplementoIdOrderByFechaRegistroDesc(UUID implementoId);

    boolean existsByImplementoIdAndEstado(UUID implementoId, EstadoIncidencia estado);
}
