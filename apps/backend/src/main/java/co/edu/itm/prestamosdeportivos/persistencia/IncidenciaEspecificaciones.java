package co.edu.itm.prestamosdeportivos.persistencia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import co.edu.itm.prestamosdeportivos.dominio.Incidencia;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** Construye filtros del histórico sin exponer {@code Specification} fuera de persistencia. */
public final class IncidenciaEspecificaciones {

    private IncidenciaEspecificaciones() {
    }

    public static Specification<Incidencia> porFiltros(IncidenciaFiltros filtros) {
        return (root, query, builder) -> {
            Join<Incidencia, Prestamo> prestamo = root.join("prestamo");
            var usuario = prestamo.join("usuario");
            var implemento = root.join("implemento");
            List<Predicate> predicados = new ArrayList<>();

            if (filtros.prestamoId() != null) {
                predicados.add(builder.equal(prestamo.get("id"), filtros.prestamoId()));
            }
            if (filtros.usuarioId() != null) {
                predicados.add(builder.equal(usuario.get("id"), filtros.usuarioId()));
            }
            if (filtros.implementoId() != null) {
                predicados.add(builder.equal(implemento.get("id"), filtros.implementoId()));
            }
            if (filtros.tipo() != null) {
                predicados.add(builder.equal(root.get("tipo"), filtros.tipo()));
            }
            if (filtros.estado() != null) {
                predicados.add(builder.equal(root.get("estado"), filtros.estado()));
            }
            if (filtros.desde() != null) {
                predicados.add(builder.greaterThanOrEqualTo(root.get("fechaRegistro"), filtros.desde()));
            }
            if (filtros.hasta() != null) {
                predicados.add(builder.lessThan(root.get("fechaRegistro"), filtros.hasta()));
            }
            if (filtros.texto() != null && !filtros.texto().isBlank()) {
                String texto = "%" + normalizar(filtros.texto()) + "%";
                predicados.add(builder.or(
                        contiene(root.get("codigo"), texto, builder),
                        contiene(implemento.get("codigoInventario"), texto, builder),
                        contiene(usuario.get("identificacion"), texto, builder),
                        contiene(usuario.get("nombres"), texto, builder),
                        contiene(usuario.get("apellidos"), texto, builder)));
            }
            return predicados.isEmpty() ? builder.conjunction() : builder.and(predicados.toArray(Predicate[]::new));
        };
    }

    private static Predicate contiene(Expression<String> campo, String patron,
            jakarta.persistence.criteria.CriteriaBuilder builder) {
        return builder.like(builder.function("unaccent", String.class, builder.lower(campo)), patron);
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
