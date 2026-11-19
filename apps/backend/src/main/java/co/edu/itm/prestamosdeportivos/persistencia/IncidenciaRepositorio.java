package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidenciaRepositorio extends JpaRepository<Incidencia, UUID> {

    Optional<Incidencia> findByCodigo(String codigo);

    List<Incidencia> findByPrestamoIdOrderByFechaRegistroDesc(UUID prestamoId);

    List<Incidencia> findByImplementoIdOrderByFechaRegistroDesc(UUID implementoId);
}
