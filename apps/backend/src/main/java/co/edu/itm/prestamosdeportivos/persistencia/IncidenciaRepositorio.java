package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Incidencia;
import co.edu.itm.prestamosdeportivos.dominio.EstadoIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface IncidenciaRepositorio extends JpaRepository<Incidencia, UUID> {

    Optional<Incidencia> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"implemento", "prestador"})
    List<Incidencia> findByPrestamoIdOrderByFechaRegistroDesc(UUID prestamoId);

    List<Incidencia> findByImplementoIdOrderByFechaRegistroDesc(UUID implementoId);

    boolean existsByImplementoIdAndEstado(UUID implementoId, EstadoIncidencia estado);
}
