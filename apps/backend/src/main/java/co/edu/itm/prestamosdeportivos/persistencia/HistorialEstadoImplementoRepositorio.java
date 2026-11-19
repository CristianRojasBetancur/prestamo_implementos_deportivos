package co.edu.itm.prestamosdeportivos.persistencia;

import co.edu.itm.prestamosdeportivos.dominio.HistorialEstadoImplemento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoImplementoRepositorio extends JpaRepository<HistorialEstadoImplemento, java.util.UUID> {
}
