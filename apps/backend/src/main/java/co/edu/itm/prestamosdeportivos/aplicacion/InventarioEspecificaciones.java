package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.Locale;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import org.springframework.data.jpa.domain.Specification;

/** Filtros combinables de consulta del inventario. */
final class InventarioEspecificaciones {

    private InventarioEspecificaciones() {
    }

    static Specification<ImplementoDeportivo> filtros(String q, UUID categoriaId, EstadoImplemento estado) {
        Specification<ImplementoDeportivo> especificacion = (raiz, consulta, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String patron = "%" + normalizar(q) + "%";
            especificacion = especificacion.and((raiz, consulta, cb) -> {
                var codigo = cb.function("translate", String.class, cb.lower(raiz.get("codigoInventario")),
                        cb.literal("áéíóúüñ"), cb.literal("aeiouun"));
                var nombre = cb.function("translate", String.class, cb.lower(raiz.get("nombre")),
                        cb.literal("áéíóúüñ"), cb.literal("aeiouun"));
                return cb.or(cb.like(codigo, patron), cb.like(nombre, patron));
            });
        }
        if (categoriaId != null) {
            especificacion = especificacion.and((raiz, consulta, cb) ->
                    cb.equal(raiz.get("categoria").get("id"), categoriaId));
        }
        if (estado != null) {
            especificacion = especificacion.and((raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado));
        }
        return especificacion;
    }

    private static String normalizar(String valor) {
        String minusculas = valor.toLowerCase(Locale.ROOT);
        return minusculas.replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u').replace('ñ', 'n');
    }
}
