package co.edu.itm.prestamosdeportivos.web.comun;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Respuesta paginada estándar (BE-01 §4.5). Nunca se serializa un {@link Page} directamente. */
public record PaginaRespuesta<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> PaginaRespuesta<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaRespuesta<>(pagina.getContent().stream().map(conversor).toList(), pagina.getNumber(),
                pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
