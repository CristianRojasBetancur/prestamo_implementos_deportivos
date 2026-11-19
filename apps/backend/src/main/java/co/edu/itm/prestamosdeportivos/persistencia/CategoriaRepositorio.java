package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepositorio extends JpaRepository<Categoria, UUID> {

    /** Usa el índice único {@code uq_categoria_nombre} sobre {@code lower(nombre)}. */
    @Query("select c from Categoria c where lower(c.nombre) = lower(:nombre)")
    Optional<Categoria> buscarPorNombre(@Param("nombre") String nombre);

    List<Categoria> findByActivaTrueOrderByNombreAsc();
}
