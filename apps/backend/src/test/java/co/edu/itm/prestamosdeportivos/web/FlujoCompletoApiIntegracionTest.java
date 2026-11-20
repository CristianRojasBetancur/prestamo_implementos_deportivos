package co.edu.itm.prestamosdeportivos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.aplicacion.PrestamoServicio;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.soporte.FabricaFixtures;
import co.edu.itm.prestamosdeportivos.soporte.FixturesConfiguracion;
import co.edu.itm.prestamosdeportivos.soporte.FixturesConfiguracion.RelojPruebas;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Escenarios HTTP completos sobre PostgreSQL real. Cada test crea su propio
 * escenario con identificadores aislados y no depende de datos locales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ContenedoresPrueba.class, FixturesConfiguracion.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlujoCompletoApiIntegracionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private FabricaFixtures fixtures;
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PrestamoServicio prestamos;
    @Autowired private RelojPruebas reloj;

    private FabricaFixtures.Escenario escenario;

    @BeforeEach
    void prepararEscenario() {
        reloj.fijar(FixturesConfiguracion.AHORA);
        escenario = fixtures.escenario();
    }

    @Test
    @DisplayName("HU-03/HU-14/HU-16/HU-18/RN-07: API completa de administración, préstamo, devolución, incidencia y cierre")
    void flujoCompleto() throws Exception {
        String admin = login(escenario.admin());
        String prestador = login(escenario.prestador());

        String nuevaIdentificacion = "USR-" + escenario.admin().identificador();
        MvcResult altaUsuario = mvc.perform(post("/api/v1/usuarios").with(bearer(prestador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificacion\":\"" + nuevaIdentificacion
                        + "\",\"nombres\":\"Nuevo\",\"apellidos\":\"Solicitante\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn();
        UUID nuevoUsuarioId = id(altaUsuario);
        mvc.perform(get("/api/v1/usuarios/" + nuevoUsuarioId + "/situacion").with(bearer(prestador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.id").value(nuevoUsuarioId.toString()));

        String nuevoPrestador = "NEW-" + escenario.admin().identificador();
        String nuevoCorreo = "nuevo." + escenario.admin().identificador().toLowerCase() + "@pruebas.invalid";
        MvcResult altaPrestador = mvc.perform(post("/api/v1/providers").with(bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombres":"Nuevo","apellidos":"Prestador","identificacion":"%s",
                         "correo":"%s","contrasenaInicial":"Nueva#2026"}
                        """.formatted(nuevoPrestador, nuevoCorreo)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID nuevoId = id(altaPrestador);
        mvc.perform(get("/api/v1/providers/" + nuevoId).with(bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.identificacion").value(nuevoPrestador));
        mvc.perform(patch("/api/v1/providers/" + nuevoId + "/estado").with(bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO"));

        MvcResult categoria = mvc.perform(post("/api/v1/categorias").with(bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Raquetas " + escenario.admin().identificador()
                        + "\",\"descripcion\":\"Categoría API\",\"activa\":true}"))
                .andExpect(status().isCreated()).andReturn();
        UUID categoriaId = id(categoria);
        mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").isNotEmpty());

        MvcResult unidadCreada = mvc.perform(post("/api/v1/inventory/units").with(bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoInventario\":\"RAQ-" + escenario.admin().identificador()
                        + "\",\"nombre\":\"Raqueta API\",\"categoriaId\":\"" + categoriaId
                        + "\",\"estado\":\"DISPONIBLE\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andReturn();
        UUID unidadCreadaId = id(unidadCreada);
        mvc.perform(get("/api/v1/inventory/units").with(bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isArray());
        mvc.perform(get("/api/v1/availability")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray());

        mvc.perform(get("/api/v1/usuarios/" + escenario.usuario().getId() + "/situacion")
                .with(bearer(prestador))).andExpect(status().isOk());

        String unidad1 = escenario.unidades().get(0).getId().toString();
        String unidad2 = escenario.unidades().get(1).getId().toString();
        String unidad3 = escenario.unidades().get(2).getId().toString();
        MvcResult altaPrestamo = mvc.perform(post("/api/v1/prestamos").with(bearer(prestador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":\"" + escenario.usuario().getId()
                        + "\",\"implementoIds\":[\"" + unidad1 + "\",\"" + unidad2 + "\",\"" + unidad3 + "\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andReturn();
        UUID prestamoId = id(altaPrestamo);

        mvc.perform(post("/api/v1/prestamos/" + prestamoId + "/devoluciones").with(bearer(prestador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"implementoId\":\"" + unidad1
                        + "\",\"condicion\":\"BUENO\"}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.alcance").value("PARCIAL"));

        MvcResult incidencia = mvc.perform(post("/api/v1/prestamos/" + prestamoId + "/incidencias")
                .with(bearer(prestador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"implementoId\":\"" + unidad2
                        + "\",\"tipo\":\"PERDIDA\",\"descripcion\":\"No fue localizada\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("PERDIDA"))
                .andReturn();
        UUID incidenciaId = id(incidencia);

        // Se avanza el reloj después de crear el préstamo para mantener válida la restricción
        // fecha_prestamo < fecha_limite_devolucion y probar el vencimiento de forma reproducible.
        reloj.fijar(FixturesConfiguracion.AHORA.plusSeconds(300));
        jdbc.update("UPDATE prestamo SET fecha_limite_devolucion = ? WHERE id = ?",
                Timestamp.from(FixturesConfiguracion.AHORA.plusSeconds(120)), prestamoId);
        mvc.perform(post("/api/v1/prestamos/" + prestamoId + "/devoluciones").with(bearer(prestador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"implementoId\":\"" + unidad3
                        + "\",\"condicion\":\"BUENO\"}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.alcance").value("COMPLETA"))
                .andExpect(jsonPath("$.sancion").isNotEmpty());

        mvc.perform(get("/api/v1/prestamos/" + prestamoId).with(bearer(prestador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.unidadesPendientes").value(0));
        mvc.perform(get("/api/v1/incidencias/" + incidenciaId).with(bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REGISTRADA"));
        mvc.perform(get("/api/v1/incidencias").with(bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isArray());
        mvc.perform(patch("/api/v1/incidencias/" + incidenciaId + "/resolucion").with(bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RESUELTA"));
        mvc.perform(get("/api/v1/devoluciones").with(bearer(prestador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido").isArray());
        mvc.perform(get("/api/v1/devoluciones").with(bearer(admin)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/auth/logout").with(bearer(prestador))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").with(bearer(prestador)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("SESION_INVALIDA"));
        assertThat(unidadCreadaId).isNotNull();
    }

    @Test
    @DisplayName("HU-14/RN-04/RN-05/RNF-05: errores de seguridad, elegibilidad, duplicado y operación repetida no dejan cambios parciales")
    void erroresCriticos() throws Exception {
        String admin = login(escenario.admin());
        String prestador = login(escenario.prestador());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificador\":\"" + escenario.prestador().correo()
                        + "\",\"contrasena\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));

        mvc.perform(post("/api/v1/categorias").with(bearer(prestador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"No autorizado\"}"))
                .andExpect(status().isForbidden());

        String bodyPrestador = "{\"nombres\":\"Duplicado\",\"apellidos\":\"Fixture\",\"identificacion\":\""
                + escenario.prestador().identificador() + "\",\"correo\":\"duplicado@pruebas.invalid\","
                + "\"contrasenaInicial\":\"Nueva#2026\"}";
        mvc.perform(post("/api/v1/providers").with(bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(bodyPrestador)).andExpect(status().isConflict());

        String unidad = escenario.unidades().get(0).getId().toString();
        String prestamo = id(mvc.perform(post("/api/v1/prestamos").with(bearer(prestador))
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":\"" + escenario.usuario().getId()
                        + "\",\"implementoIds\":[\"" + unidad + "\"]}"))
                .andExpect(status().isCreated()).andReturn()).toString();
        mvc.perform(post("/api/v1/prestamos").with(bearer(prestador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":\"" + escenario.usuario().getId() + "\",\"implementoIds\":[\""
                        + unidad + "\"]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("IMPLEMENTO_NO_DISPONIBLE"));

        String ajena = escenario.unidades().get(1).getId().toString();
        mvc.perform(post("/api/v1/prestamos/" + prestamo + "/incidencias").with(bearer(prestador))
                .contentType(MediaType.APPLICATION_JSON).content("{\"implementoId\":\"" + ajena
                        + "\",\"tipo\":\"PERDIDA\",\"descripcion\":\"Unidad ajena\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO"));

        mvc.perform(post("/api/v1/prestamos/" + prestamo + "/devoluciones").with(bearer(prestador))
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"implementoId\":\"" + unidad
                        + "\",\"condicion\":\"BUENO\"}]}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/prestamos/" + prestamo + "/devoluciones").with(bearer(prestador))
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"implementoId\":\"" + unidad
                        + "\",\"condicion\":\"BUENO\"}]}"))
                .andExpect(status().isConflict());

        Usuario suspendido = fixtures.usuario("SUSP-" + escenario.admin().identificador(), "Suspendido", "Fixture",
                Rol.USER, null, null);
        suspendido.extenderSuspensionHasta(FixturesConfiguracion.AHORA.plusSeconds(3600));
        usuarios.saveAndFlush(suspendido);
        mvc.perform(post("/api/v1/prestamos").with(bearer(prestador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":\"" + suspendido.getId() + "\",\"implementoIds\":[\""
                        + escenario.unidades().get(2).getId() + "\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("USUARIO_SANCIONADO"));
    }

    @Test
    @DisplayName("RN-05: un usuario con préstamo vencido es rechazado por la API")
    void usuarioVencido() throws Exception {
        String prestador = login(escenario.prestador());
        UsuarioAutenticado operador = new UsuarioAutenticado(escenario.prestador().id(), Rol.PRESTADOR);
        var previo = prestamos.registrar(escenario.usuario().getId(),
                java.util.List.of(escenario.unidades().get(0).getId()), operador);
        reloj.fijar(FixturesConfiguracion.AHORA.plusSeconds(300));
        jdbc.update("UPDATE prestamo SET fecha_limite_devolucion = ? WHERE id = ?",
                Timestamp.from(FixturesConfiguracion.AHORA.plusSeconds(120)), previo.id());

        mvc.perform(post("/api/v1/prestamos").with(bearer(prestador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":\"" + escenario.usuario().getId() + "\",\"implementoIds\":[\""
                        + escenario.unidades().get(1).getId() + "\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("USUARIO_CON_PRESTAMOS_VENCIDOS"));
    }

    private String login(FabricaFixtures.Cuenta cuenta) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificador\":\"" + cuenta.correo() + "\",\"contrasena\":\""
                        + cuenta.contrasena() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").doesNotExist())
                .andReturn();
        JsonNode respuesta = json.readTree(resultado.getResponse().getContentAsString());
        assertThat(respuesta.path("usuario").path("rol").asText()).isEqualTo(cuenta.usuario().getRol().name());
        return respuesta.path("tokenAcceso").asText();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    private UUID id(MvcResult resultado) throws Exception {
        return UUID.fromString(json.readTree(resultado.getResponse().getContentAsString()).path("id").asText());
    }
}
