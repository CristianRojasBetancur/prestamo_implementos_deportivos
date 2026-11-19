package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevolucionRepositorio extends JpaRepository<Devolucion, UUID> {

    Optional<Devolucion> findByCodigo(String codigo);

    List<Devolucion> findByPrestamoIdOrderByFechaDevolucionDesc(UUID prestamoId);
}
