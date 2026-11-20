package co.edu.itm.prestamosdeportivos.persistencia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestador;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public final class UsuarioEspecificaciones {

    private UsuarioEspecificaciones() { }

    public static Specification<Usuario> prestadores(String q, String nombres, String identificacion,
            String correo, EstadoPrestador estado) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("rol"), Rol.PRESTADOR));

            agregarContiene(predicados, root.get("nombres"), nombres, cb);
            agregarContiene(predicados, root.get("identificacion"), identificacion, cb);
            agregarContiene(predicados, root.get("correo"), correo, cb);

            if (estado != null) {
                predicados.add(cb.equal(root.get("activo"), estado == EstadoPrestador.ACTIVO));
            }
            if (noVacio(q)) {
                String patron = "%" + normalizar(q) + "%";
                predicados.add(cb.or(
                        contiene(root.get("nombres"), patron, cb),
                        contiene(root.get("apellidos"), patron, cb),
                        contiene(root.get("identificacion"), patron, cb),
                        contiene(root.get("correo"), patron, cb)));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    public static Specification<Usuario> solicitantes(String q) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("rol"), Rol.USER));
            if (noVacio(q)) {
                String patron = "%" + normalizar(q) + "%";
                predicados.add(cb.or(
                        contiene(root.get("nombres"), patron, cb),
                        contiene(root.get("apellidos"), patron, cb),
                        contiene(root.get("identificacion"), patron, cb)));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private static void agregarContiene(List<Predicate> predicados, Expression<String> campo,
            String valor, jakarta.persistence.criteria.CriteriaBuilder cb) {
        if (noVacio(valor)) {
            predicados.add(contiene(campo, "%" + normalizar(valor) + "%", cb));
        }
    }

    private static Predicate contiene(Expression<String> campo, String patron,
            jakarta.persistence.criteria.CriteriaBuilder cb) {
        Expression<String> sinTildes = cb.function("unaccent", String.class, cb.lower(campo));
        return cb.like(sinTildes, patron);
    }

    private static boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
