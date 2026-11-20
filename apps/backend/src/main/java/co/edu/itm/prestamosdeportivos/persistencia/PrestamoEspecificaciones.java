package co.edu.itm.prestamosdeportivos.persistencia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public final class PrestamoEspecificaciones {

    private PrestamoEspecificaciones() {
    }

    public static Specification<Prestamo> porFiltros(PrestamoFiltros filtros) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (filtros.estados() != null && !filtros.estados().isEmpty()) {
                List<Predicate> estados = new ArrayList<>();
                var subconsulta = query.subquery(java.util.UUID.class);
                var detalle = subconsulta.from(co.edu.itm.prestamosdeportivos.dominio.DetallePrestamo.class);
                subconsulta.select(detalle.get("prestamo").get("id"));
                subconsulta.where(
                        cb.equal(detalle.get("prestamo").get("id"), root.get("id")),
                        cb.equal(detalle.get("estado"), EstadoDetallePrestamo.PENDIENTE_DEVOLUCION));
                Predicate vencidoCalculado = cb.and(
                        cb.notEqual(root.get("estado"), EstadoPrestamo.FINALIZADO),
                        cb.lessThan(root.get("fechaLimiteDevolucion"), filtros.ahora()),
                        cb.exists(subconsulta));
                for (EstadoPrestamo estado : filtros.estados()) {
                    if (estado == EstadoPrestamo.VENCIDO) {
                        estados.add(cb.or(cb.equal(root.get("estado"), EstadoPrestamo.VENCIDO), vencidoCalculado));
                    } else {
                        estados.add(cb.and(cb.equal(root.get("estado"), estado), cb.not(vencidoCalculado)));
                    }
                }
                predicados.add(cb.or(estados.toArray(Predicate[]::new)));
            }
            if (filtros.usuarioId() != null) {
                predicados.add(cb.equal(root.get("usuario").get("id"), filtros.usuarioId()));
            }
            if (noVacio(filtros.identificacion())) {
                predicados.add(cb.equal(root.get("usuario").get("identificacion"), filtros.identificacion().trim()));
            }
            if (filtros.desde() != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaPrestamo"), filtros.desde()));
            }
            if (filtros.hasta() != null) {
                predicados.add(cb.lessThan(root.get("fechaPrestamo"), filtros.hasta()));
            }
            if (noVacio(filtros.texto())) {
                String patron = "%" + normalizar(filtros.texto()) + "%";
                predicados.add(cb.or(
                        contiene(root.get("codigo"), patron, cb),
                        contiene(root.get("usuario").get("nombres"), patron, cb),
                        contiene(root.get("usuario").get("apellidos"), patron, cb),
                        contiene(root.get("usuario").get("identificacion"), patron, cb),
                        contiene(root.get("prestador").get("nombres"), patron, cb),
                        contiene(root.get("prestador").get("apellidos"), patron, cb)));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private static Predicate contiene(Expression<String> campo, String patron,
            jakarta.persistence.criteria.CriteriaBuilder cb) {
        return cb.like(cb.function("unaccent", String.class, cb.lower(campo)), patron);
    }

    private static boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
