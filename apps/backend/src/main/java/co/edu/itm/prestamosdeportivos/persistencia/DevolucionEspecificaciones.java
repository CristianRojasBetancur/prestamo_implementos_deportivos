package co.edu.itm.prestamosdeportivos.persistencia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import co.edu.itm.prestamosdeportivos.dominio.Devolucion;
import co.edu.itm.prestamosdeportivos.dominio.DetalleDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.DetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import org.springframework.data.jpa.domain.Specification;

/** Construye filtros sin exponer {@code Specification} fuera de persistencia. */
public final class DevolucionEspecificaciones {

    private DevolucionEspecificaciones() {
    }

    public static Specification<Devolucion> porFiltros(DevolucionFiltros filtros) {
        return (root, query, builder) -> {
            query.distinct(true);
            Join<Devolucion, Prestamo> prestamo = root.join("prestamo");
            Join<Devolucion, DetalleDevolucion> detalle = root.join("detalles", JoinType.LEFT);
            Join<DetalleDevolucion, DetallePrestamo> detallePrestamo = detalle.join("detallePrestamo", JoinType.LEFT);
            var implemento = detallePrestamo.join("implemento", JoinType.LEFT);
            var usuario = prestamo.join("usuario");
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
            if (filtros.condicion() != null) {
                predicados.add(builder.equal(detalle.get("condicion"), filtros.condicion()));
            }
            if (filtros.alcance() != null) {
                predicados.add(builder.equal(root.get("alcance"), filtros.alcance()));
            }
            if (filtros.desde() != null) {
                predicados.add(builder.greaterThanOrEqualTo(root.get("fechaDevolucion"), filtros.desde()));
            }
            if (filtros.hasta() != null) {
                predicados.add(builder.lessThan(root.get("fechaDevolucion"), filtros.hasta()));
            }
            if (filtros.texto() != null && !filtros.texto().isBlank()) {
                String texto = "%" + normalizar(filtros.texto()) + "%";
                predicados.add(builder.or(
                        contiene(root.get("codigo"), texto, builder),
                        contiene(prestamo.get("codigo"), texto, builder),
                        contiene(usuario.get("identificacion"), texto, builder),
                        contiene(usuario.get("nombres"), texto, builder),
                        contiene(usuario.get("apellidos"), texto, builder),
                        contiene(implemento.get("codigoInventario"), texto, builder)));
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
