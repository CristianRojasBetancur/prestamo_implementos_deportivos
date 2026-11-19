package co.edu.itm.prestamosdeportivos.aplicacion;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.persistencia.DisponibilidadProyeccion;
import co.edu.itm.prestamosdeportivos.persistencia.DisponibilidadRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CU-12: búsqueda pública de productos con unidades actualmente prestables. */
@Service
public class DisponibilidadServicio {

    private static final Set<String> CAMPOS_ORDEN = Set.of("nombre", "categoriaNombre", "unidadesDisponibles");
    private final DisponibilidadRepositorio disponibilidad;

    public DisponibilidadServicio(DisponibilidadRepositorio disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @Transactional(readOnly = true)
    public Page<DisponibilidadProyeccion> consultar(String q, UUID categoriaId, Pageable pageable) {
        Pageable estable = ordenar(pageable);
        return disponibilidad.buscarDisponibles(normalizar(q), categoriaId, estable);
    }

    public Set<String> camposOrdenPermitidos() {
        return CAMPOS_ORDEN;
    }

    private static Pageable ordenar(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by("nombre").ascending();
        }
        // Completa cualquier orden solicitado para que las páginas no se solapen ni omitan filas.
        if (sort.getOrderFor("categoriaNombre") == null) {
            sort = sort.and(Sort.by("categoriaNombre").ascending());
        }
        if (sort.getOrderFor("categoriaId") == null) {
            sort = sort.and(Sort.by("categoriaId").ascending());
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static String normalizar(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return Normalizer.normalize(q.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
