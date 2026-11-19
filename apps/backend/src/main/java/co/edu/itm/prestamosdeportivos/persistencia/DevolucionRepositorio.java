package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Devolucion;
import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DevolucionRepositorio extends JpaRepository<Devolucion, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Devolucion> {

    Optional<Devolucion> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"prestador"})
    List<Devolucion> findByPrestamoIdOrderByFechaDevolucionDesc(UUID prestamoId);

    @Override
    @EntityGraph(attributePaths = {"prestamo", "prestamo.usuario", "prestamo.prestador", "prestador"})
    Page<Devolucion> findAll(Specification<Devolucion> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"prestamo", "prestamo.usuario", "prestamo.prestador", "prestador",
            "detalles", "detalles.detallePrestamo", "detalles.detallePrestamo.implemento", "detalles.incidencia"})
    Optional<Devolucion> findById(UUID id);

    interface ConteoDetalles {
        UUID getDevolucionId();
        long getCantidad();
        long getConNovedad();
    }

    interface ImplementoDevuelto {
        UUID getDevolucionId();
        UUID getImplementoId();
        String getCodigoInventario();
        String getNombre();
    }

    @Query("select d.devolucion.id as devolucionId, count(d.id) as cantidad, "
            + "sum(case when d.condicion = :condicion then 1 else 0 end) as conNovedad "
            + "from DetalleDevolucion d where d.devolucion.id in :ids group by d.devolucion.id")
    List<ConteoDetalles> contarDetalles(@Param("ids") List<UUID> ids,
            @Param("condicion") CondicionDevolucion condicion);

    @Query("select d.devolucion.id as devolucionId, i.id as implementoId, "
            + "i.codigoInventario as codigoInventario, i.nombre as nombre "
            + "from DetalleDevolucion d join d.detallePrestamo dp join dp.implemento i "
            + "where d.devolucion.id in :ids order by d.devolucion.id, i.id")
    List<ImplementoDevuelto> implementosDevolucion(@Param("ids") List<UUID> ids);
}
