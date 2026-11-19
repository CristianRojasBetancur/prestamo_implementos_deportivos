package co.edu.itm.prestamosdeportivos.persistencia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Agregación y paginación de disponibilidad ejecutadas íntegramente en PostgreSQL. */
@Repository
public class DisponibilidadRepositorio {

    private static final String FROM_FILTROS = """
            FROM implemento_deportivo i
            JOIN categoria c ON c.id = i.categoria_id
            WHERE c.activa = true
              AND i.estado <> 'DADO_DE_BAJA'
              AND NOT EXISTS (
                  SELECT 1 FROM incidencia inc
                   WHERE inc.implemento_id = i.id AND inc.estado = 'REGISTRADA'
              )
            """;

    private static final String GROUP_HAVING = """
            GROUP BY i.nombre, c.id, c.nombre
            HAVING COUNT(*) FILTER (WHERE i.estado = 'DISPONIBLE') > 0
            """;

    private static final Map<String, String> COLUMNAS_ORDEN = Map.of(
            "nombre", "i.nombre",
            "categoriaNombre", "c.nombre",
            "categoriaId", "c.id",
            "unidadesDisponibles", "unidades_disponibles");

    private final EntityManager entityManager;

    public DisponibilidadRepositorio(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<DisponibilidadProyeccion> buscarDisponibles(String q, UUID categoriaId, Pageable pageable) {
        String filtros = filtros(q, categoriaId);
        String contenidoSql = """
                SELECT i.nombre,
                       c.id,
                       c.nombre,
                       COUNT(*) FILTER (WHERE i.estado = 'DISPONIBLE') AS unidades_disponibles,
                       COUNT(*) AS unidades_totales,
                       (COUNT(*) FILTER (WHERE i.estado = 'DISPONIBLE') > 0) AS disponible
                """ + FROM_FILTROS + filtros + "\n" + GROUP_HAVING + " ORDER BY " + orden(pageable.getSort());
        Query consulta = entityManager.createNativeQuery(contenidoSql);
        asignarParametros(consulta, q, categoriaId);
        consulta.setFirstResult(Math.toIntExact(pageable.getOffset()));
        consulta.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> filas = consulta.getResultList();
        List<DisponibilidadProyeccion> contenido = filas.stream().map(DisponibilidadRepositorio::proyeccion).toList();

        String conteoSql = "SELECT COUNT(*) FROM (SELECT i.nombre, c.id, c.nombre " + FROM_FILTROS + filtros
                + "\n" + GROUP_HAVING + ") grupos";
        Query conteo = entityManager.createNativeQuery(conteoSql);
        asignarParametros(conteo, q, categoriaId);
        long total = ((Number) conteo.getSingleResult()).longValue();
        return new PageImpl<>(contenido, pageable, total);
    }

    private static String filtros(String q, UUID categoriaId) {
        StringBuilder filtros = new StringBuilder();
        if (categoriaId != null) {
            filtros.append(" AND c.id = :categoriaId");
        }
        if (q != null && !q.isBlank()) {
            filtros.append(" AND (translate(lower(i.nombre), 'áéíóúüñ', 'aeiouun') LIKE translate(lower(:q), 'áéíóúüñ', 'aeiouun')")
                    .append(" OR translate(lower(c.nombre), 'áéíóúüñ', 'aeiouun') LIKE translate(lower(:q), 'áéíóúüñ', 'aeiouun'))");
        }
        return filtros.toString();
    }

    private static void asignarParametros(Query consulta, String q, UUID categoriaId) {
        if (categoriaId != null) {
            consulta.setParameter("categoriaId", categoriaId);
        }
        if (q != null && !q.isBlank()) {
            consulta.setParameter("q", "%" + q + "%");
        }
    }

    private static String orden(Sort sort) {
        List<String> columnas = new ArrayList<>();
        Sort efectivo = sort.isUnsorted() ? Sort.by("nombre").ascending() : sort;
        for (Sort.Order order : efectivo) {
            String columna = COLUMNAS_ORDEN.get(order.getProperty());
            if (columna == null) {
                throw new IllegalArgumentException("Orden no permitido en disponibilidad.");
            }
            columnas.add(columna + (order.isDescending() ? " DESC" : " ASC"));
        }
        if (efectivo.getOrderFor("categoriaNombre") == null) {
            columnas.add("c.nombre ASC");
        }
        if (efectivo.getOrderFor("categoriaId") == null) {
            columnas.add("c.id ASC");
        }
        return String.join(", ", columnas);
    }

    private static DisponibilidadProyeccion proyeccion(Object[] fila) {
        return new DisponibilidadFila((String) fila[0], (UUID) fila[1], (String) fila[2],
                ((Number) fila[3]).longValue(), ((Number) fila[4]).longValue(), (Boolean) fila[5]);
    }
}
