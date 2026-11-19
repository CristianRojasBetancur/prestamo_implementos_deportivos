package co.edu.itm.prestamosdeportivos.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.dominio.RolUsuario;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;

@DataJpaTest
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@Transactional
class UsuarioRepositorioTest {

    @Autowired UsuarioRepositorio usuarios;

    @Test
    @DisplayName("CU-04 pagina resultados deterministas y filtra sin distinguir tildes")
    void filtraYPaginaPrestadores() {
        Instant ahora = Instant.parse("2026-11-18T16:00:00Z");
        usuarios.save(new Usuario("1001", "Maria", "Rodriguez", "maria@itm.edu.co", "hash",
                RolUsuario.PRESTADOR, true, ahora));
        usuarios.save(new Usuario("1002", "María José", "López", "maria.jose@itm.edu.co", "hash",
                RolUsuario.PRESTADOR, true, ahora));
        usuarios.save(new Usuario("1003", "María", "ADMIN", "admin@itm.edu.co", "hash",
                RolUsuario.ADMIN, true, ahora));
        usuarios.flush();

        var pagina = usuarios.findAll(
                UsuarioEspecificaciones.prestadores("", "maria", null, null, null),
                PageRequest.of(0, 1, Sort.by("nombres").ascending().and(Sort.by("id").ascending())));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().getFirst().getRol()).isEqualTo(RolUsuario.PRESTADOR);
        assertThat(pagina.getContent().getFirst().getNombres()).isEqualTo("Maria");
    }
}
