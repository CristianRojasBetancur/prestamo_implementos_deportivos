package co.edu.itm.prestamosdeportivos.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.dominio.RolUsuario;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@Transactional
class PrestadorControladorTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepositorio usuarios;

    @Test
    @DisplayName("Criterio 6: sin autenticación las rutas de prestadores responden 401")
    void exigeAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/providers"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Criterio 6: USER no puede administrar prestadores")
    void usuarioNoPuedeAdministrar() throws Exception {
        mvc.perform(get("/api/v1/providers").with(user("user").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("Criterio 6: PRESTADOR no puede administrar prestadores")
    void prestadorNoPuedeAdministrar() throws Exception {
        mvc.perform(get("/api/v1/providers").with(user("prestador").roles("PRESTADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CU-03 y criterio 1: ADMIN registra con 201, Location y sin exponer hash")
    void adminRegistraPrestador() throws Exception {
        mvc.perform(post("/api/v1/providers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("1001", "maria@itm.edu.co", "María")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/providers/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombres").value("María"))
                .andExpect(jsonPath("$.rol").value("PRESTADOR"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.creadoEn").isNotEmpty())
                .andExpect(jsonPath("$.actualizadoEn").isNotEmpty())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist())
                .andExpect(jsonPath("$.contrasenaInicial").doesNotExist());
    }

    @Test
    @DisplayName("Criterio 2: identificación duplicada devuelve 409 y señala el campo")
    void rechazaDuplicado() throws Exception {
        mvc.perform(post("/api/v1/providers").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(registro("1002", "ana@itm.edu.co", "Ana")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/providers").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(registro("1002", "otra@itm.edu.co", "Otra")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
                .andExpect(jsonPath("$.errores[0].campo").value("identificacion"));
    }

    @Test
    @DisplayName("CU-04 y criterio 3: busca con filtros y página estable")
    void buscaFiltraYPagina() throws Exception {
        crear("1003", "maria1@itm.edu.co", "Maria");
        crear("1004", "maria2@itm.edu.co", "María José");
        crear("1005", "otro@itm.edu.co", "Pedro");

        mvc.perform(get("/api/v1/providers").with(user("admin").roles("ADMIN"))
                        .param("nombres", "maria")
                        .param("estado", "ACTIVO")
                        .param("pagina", "0")
                        .param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].nombres").value("Maria"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    @DisplayName("CU-05: PATCH modifica campos permitidos sin alterar contraseña ni rol")
    void actualizaPrestador() throws Exception {
        UUID id = crear("1006", "luz@itm.edu.co", "Luz");
        String hashOriginal = usuarios.findById(id).orElseThrow().getContrasenaHash();

        mvc.perform(patch("/api/v1/providers/{id}", id).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Luz Marina\",\"rol\":\"ADMIN\",\"contrasenaHash\":\"filtrado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Luz Marina"))
                .andExpect(jsonPath("$.rol").value("PRESTADOR"))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());

        org.assertj.core.api.Assertions.assertThat(usuarios.findById(id).orElseThrow().getContrasenaHash())
                .isEqualTo(hashOriginal);
    }

    @Test
    @DisplayName("CU-06 y criterio 5: desactiva sin borrar y versiona credenciales")
    void desactivaConservandoHistorial() throws Exception {
        UUID id = crear("1007", "sam@itm.edu.co", "Sam");

        mvc.perform(patch("/api/v1/providers/{id}/estado", id).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        Usuario guardado = usuarios.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(guardado.isActivo()).isFalse();
        org.assertj.core.api.Assertions.assertThat(guardado.getVersionCredenciales()).isEqualTo(1);

        mvc.perform(get("/api/v1/providers/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    @DisplayName("Criterio 2: campos requeridos inválidos devuelven errores por campo")
    void validaCamposRequeridos() throws Exception {
        mvc.perform(post("/api/v1/providers").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"\",\"correo\":\"correo-invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.errores.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    private UUID crear(String identificacion, String correo, String nombres) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/providers").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro(identificacion, correo, nombres)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(respuesta).get("id").asText());
    }

    private String registro(String identificacion, String correo, String nombres) throws Exception {
        return objectMapper.writeValueAsString(new RegistrarPrestadorSolicitud(nombres, "Prueba",
                identificacion, correo, "Clave-temporal-2026"));
    }
}
