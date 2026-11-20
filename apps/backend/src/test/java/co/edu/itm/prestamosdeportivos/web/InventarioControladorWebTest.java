package co.edu.itm.prestamosdeportivos.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.UUID;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.configuracion.FiltroCorrelacion;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventarioControladorWebTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private FiltroCorrelacion filtroCorrelacion;
    @Autowired private ObjectMapper objectMapper;
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtroCorrelacion)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN crea, consulta, actualiza, retira y conserva la unidad con historial")
    void cicloAdministrativoDeUnidad() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        MvcResult categoriaCreada = mvc.perform(post("/api/v1/categorias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Categoría " + sufijo + "\",\"descripcion\":\"Prueba\"}"))
                .andExpect(status().isCreated()).andReturn();
        String categoriaId = objectMapper.readTree(categoriaCreada.getResponse().getContentAsString()).get("id").asText();

        String codigo = "BAL-" + sufijo.toUpperCase();
        mvc.perform(post("/api/v1/inventory/units").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoInventario\":\"" + codigo + "\",\"nombre\":\"Balón de prueba\","
                                + "\"categoriaId\":\"" + categoriaId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.prestable").value(true))
                .andExpect(jsonPath("$.creadoEn").exists());

        mvc.perform(get("/api/v1/implementos/prestables").param("q", codigo).param("tamano", "10")
                        .with(user("prestador").roles("PRESTADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].codigoInventario").value(codigo))
                .andExpect(jsonPath("$.contenido[0].estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.contenido[0].fechaRegistro").exists());
        mvc.perform(get("/api/v1/inventory/units").with(user("prestador").roles("PRESTADOR")))
                .andExpect(status().isForbidden());

        MvcResult creada = mvc.perform(post("/api/v1/inventory/units").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoInventario\":\"" + codigo + "\",\"nombre\":\"Duplicado\","
                                + "\"categoriaId\":\"" + categoriaId + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
                .andReturn();

        mvc.perform(get("/api/v1/inventory/units").param("q", "balon").param("categoriaId", categoriaId)
                        .param("estado", "DISPONIBLE").param("pagina", "0").param("tamano", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].codigoInventario").value(codigo))
                .andExpect(jsonPath("$.totalElementos").value(1));

        var unidadId = objectMapper.readTree(
                mvc.perform(get("/api/v1/inventory/units").param("q", codigo))
                        .andReturn().getResponse().getContentAsString()).get("contenido").get(0).get("id").asText();
        mvc.perform(patch("/api/v1/inventory/units/" + unidadId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Balón actualizado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Balón actualizado"));

        mvc.perform(patch("/api/v1/inventory/units/" + unidadId + "/state").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_MANTENIMIENTO\",\"motivo\":\"Desgaste\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_MANTENIMIENTO"))
                .andExpect(jsonPath("$.prestable").value(false));
        mvc.perform(patch("/api/v1/inventory/units/" + unidadId + "/state").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"DADO_DE_BAJA\",\"motivo\":\"Fin de vida útil\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prestable").value(false));
        mvc.perform(get("/api/v1/inventory/units/" + unidadId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("DADO_DE_BAJA"));
    }
}
