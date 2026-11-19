package co.edu.itm.prestamosdeportivos.web;

import java.util.List;

public record PaginaRespuesta<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {
}
