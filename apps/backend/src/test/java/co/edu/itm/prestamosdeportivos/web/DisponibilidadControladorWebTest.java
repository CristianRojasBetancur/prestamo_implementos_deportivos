package co.edu.itm.prestamosdeportivos.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.configuracion.FiltroCorrelacion;
import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@Transactional
class DisponibilidadControladorWebTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired private FiltroCorrelacion filtroCorrelacion;
    @Autowired private CategoriaRepositorio categorias;
    @Autowired private ImplementoDeportivoRepositorio implementos;
    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtroCorrelacion)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @Test
    @DisplayName("Consulta anónima devuelve solo categorías y grupos con unidades disponibles, sin datos de unidad")
    void consultaPublicaFiltradaYAgregada() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Categoria categoria = categorias.saveAndFlush(new Categoria("Disponibilidad " + sufijo, null));
        implementos.saveAndFlush(new ImplementoDeportivo("AVL-" + sufijo, "Balón alfa", null, categoria,
                EstadoImplemento.DISPONIBLE));
        implementos.saveAndFlush(new ImplementoDeportivo("MNT-" + sufijo, "Balón beta", null, categoria,
                EstadoImplemento.EN_MANTENIMIENTO));
        implementos.saveAndFlush(new ImplementoDeportivo("RET-" + sufijo, "Balón gamma", null, categoria,
                EstadoImplemento.DADO_DE_BAJA));
        implementos.saveAndFlush(new ImplementoDeportivo("LND-" + sufijo, "Balón delta", null, categoria,
                EstadoImplemento.PRESTADO));

        mvc.perform(get("/api/v1/availability").param("categoryId", categoria.getId().toString())
                        .param("q", "balon").param("pagina", "0").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Balón alfa"))
                .andExpect(jsonPath("$.contenido[0].unidadesDisponibles").value(1))
                .andExpect(jsonPath("$.contenido[0].disponible").value(true))
                .andExpect(jsonPath("$.contenido[0].codigoInventario").doesNotExist())
                .andExpect(jsonPath("$.contenido[0].id").doesNotExist())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1));

        mvc.perform(get("/api/v1/disponibilidad").param("categoriaId", categoria.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].nombre").value("Balón alfa"));

        Categoria sinDisponibles = categorias.saveAndFlush(new Categoria("Sin disponibilidad " + sufijo, null));
        implementos.saveAndFlush(new ImplementoDeportivo("MNT2-" + sufijo, "Raqueta", null, sinDisponibles,
                EstadoImplemento.EN_MANTENIMIENTO));
        mvc.perform(get("/api/v1/availability").param("categoryId", sinDisponibles.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(0))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }
}
