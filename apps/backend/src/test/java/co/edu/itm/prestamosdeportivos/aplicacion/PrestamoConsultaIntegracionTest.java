package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@Transactional
class PrestamoConsultaIntegracionTest {

    @Autowired MockMvc mvc;
    @Autowired PrestamoServicio registro;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired CategoriaRepositorio categorias;
    @Autowired ImplementoDeportivoRepositorio implementos;
    @Autowired PrestamoRepositorio prestamos;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transacciones;

    @Test
    @DisplayName("BE-11: lista préstamos con filtros combinados, paginación y estado vencido calculado por reloj")
    void listaFiltraPaginaYCalculaVencimiento() throws Exception {
        var vencido = fixture(1);
        var vigente = fixture(1);
        var respuestaVencido = registro.registrar(vencido.usuario().getId(),
                List.of(vencido.unidades().getFirst().getId()), vencido.operador());
        registro.registrar(vigente.usuario().getId(), List.of(vigente.unidades().getFirst().getId()), vigente.operador());
        Instant ahora = Instant.now();
        transacciones.executeWithoutResult(status -> {
            entityManager.createNativeQuery("update prestamo set fecha_prestamo = :inicio, "
                    + "fecha_limite_devolucion = :limite where id = :id")
                    .setParameter("inicio", ahora.minusSeconds(48 * 3600))
                    .setParameter("limite", ahora.minusSeconds(3600))
                    .setParameter("id", respuestaVencido.id())
                    .executeUpdate();
            entityManager.clear();
        });
        mvc.perform(get("/api/v1/loans")
                .param("estado", "VENCIDO")
                .param("identificacion", vencido.usuario().getIdentificacion())
                .param("pagina", "0")
                .param("tamano", "1")
                .with(authentication(autenticacion(vencido.operador()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].estado").value("VENCIDO"))
                .andExpect(jsonPath("$.contenido[0].vencido").value(true))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(1));
    }

    @Test
    @DisplayName("BE-11: el detalle incluye unidades y no cambia el préstamo")
    void detalleCompletoNoMutaDatos() throws Exception {
        var fixture = fixture(2);
        var creado = registro.registrar(fixture.usuario().getId(),
                fixture.unidades().stream().map(u -> u.getId()).toList(), fixture.operador());
        var antes = prestamos.findById(creado.id()).orElseThrow();
        mvc.perform(get("/api/v1/loans/" + creado.id()).with(authentication(autenticacion(fixture.operador()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(creado.id().toString()))
                .andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.detalles.length()").value(2))
                .andExpect(jsonPath("$.devoluciones").isArray())
                .andExpect(jsonPath("$.incidencias").isArray());
        var despues = prestamos.findById(creado.id()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(despues.getEstado()).isEqualTo(antes.getEstado());
        org.assertj.core.api.Assertions.assertThat(despues.getFechaFinalizacion()).isEqualTo(antes.getFechaFinalizacion());
    }

    @Test
    @DisplayName("BE-11: un usuario sin préstamos conserva la respuesta 200 con colecciones vacías")
    void situacionSinPrestamos() throws Exception {
        var fixture = fixture(1);
        mvc.perform(get("/api/v1/usuarios/" + fixture.usuario().getId() + "/situacion")
                .with(authentication(autenticacion(fixture.operador()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prestamosActivos").value(0))
                .andExpect(jsonPath("$.prestamosVencidos").value(0))
                .andExpect(jsonPath("$.unidadesActivas").value(0))
                .andExpect(jsonPath("$.motivos").isArray());
    }

    @Test
    @DisplayName("BE-11: los endpoints de consulta exigen PRESTADOR")
    void autorizacion() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(get("/api/v1/prestamos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/loans/" + id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken autenticacion(UsuarioAutenticado principal) {
        return UsernamePasswordAuthenticationToken.authenticated(principal, "test",
                List.of(new SimpleGrantedAuthority("ROLE_PRESTADOR")));
    }

    private Fixture fixture(int unidadesSolicitadas) {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        var usuario = usuarios.saveAndFlush(Usuario.solicitante("u" + sufijo, "Usuario", "Prueba"));
        var prestador = usuarios.saveAndFlush(Usuario.conAcceso("p" + sufijo, "Prestador", "Prueba",
                "p" + sufijo + "@itm.edu.co", "hash", Rol.PRESTADOR));
        var categoria = categorias.saveAndFlush(new Categoria("Cat " + sufijo, null));
        var unidades = java.util.stream.IntStream.range(0, unidadesSolicitadas)
                .mapToObj(i -> implementos.saveAndFlush(new co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo(
                        "COD-" + UUID.randomUUID().toString().substring(0, 8), "Unidad de prueba", null,
                        categoria, EstadoImplemento.DISPONIBLE)))
                .toList();
        return new Fixture(usuario, unidades, new UsuarioAutenticado(prestador.getId(), Rol.PRESTADOR));
    }

    private record Fixture(Usuario usuario,
            List<co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo> unidades,
            UsuarioAutenticado operador) {
    }
}
