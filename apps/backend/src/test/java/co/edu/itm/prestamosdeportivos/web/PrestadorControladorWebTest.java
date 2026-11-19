package co.edu.itm.prestamosdeportivos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.itm.prestamosdeportivos.aplicacion.PrestadorServicio;
import co.edu.itm.prestamosdeportivos.configuracion.ConfiguracionReloj;
import co.edu.itm.prestamosdeportivos.configuracion.ConfiguracionSeguridad;
import co.edu.itm.prestamosdeportivos.configuracion.ManejadorErroresSeguridad;
import co.edu.itm.prestamosdeportivos.web.error.ManejadorGlobalErrores;

@WebMvcTest(PrestadorControlador.class)
@Import({ConfiguracionSeguridad.class, ConfiguracionReloj.class,
        ManejadorErroresSeguridad.class, ManejadorGlobalErrores.class})
class PrestadorControladorWebTest {

    @Autowired MockMvc mvc;
    @MockitoBean PrestadorServicio prestadores;

    @Test
    @DisplayName("Controller: petición anónima recibe 401")
    void anonimoNoAccede() throws Exception {
        mvc.perform(get("/api/v1/providers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        verifyNoInteractions(prestadores);
    }

    @Test
    @DisplayName("Controller: un PRESTADOR autenticado recibe 403")
    void prestadorNoAdministra() throws Exception {
        mvc.perform(get("/api/v1/providers").with(user("prestador").roles("PRESTADOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));

        verifyNoInteractions(prestadores);
    }

    @Test
    @DisplayName("Controller: ADMIN crea prestador y recibe Location")
    void adminCreaPrestador() throws Exception {
        UUID id = UUID.randomUUID();
        OffsetDateTime fecha = OffsetDateTime.parse("2026-11-18T10:00:00-05:00");
        when(prestadores.registrar(any())).thenReturn(new PrestadorRespuesta(id, "Ana", "Pérez",
                "1001", "ana@itm.edu.co", "PRESTADOR", "ACTIVO", fecha, fecha));

        mvc.perform(post("/api/v1/providers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombres":"Ana","apellidos":"Pérez","identificacion":"1001",
                                "correo":"ana@itm.edu.co","contrasenaInicial":"Clave-Temporal-2026"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/providers/" + id))
                .andExpect(jsonPath("$.rol").value("PRESTADOR"))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    @DisplayName("Controller: la validación bloquea datos inválidos antes del servicio")
    void validaAntesDeDelegar() throws Exception {
        mvc.perform(post("/api/v1/providers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"\",\"correo\":\"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.errores.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        verifyNoInteractions(prestadores);
    }
}
