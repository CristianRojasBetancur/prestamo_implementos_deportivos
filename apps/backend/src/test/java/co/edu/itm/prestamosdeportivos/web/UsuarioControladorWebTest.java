package co.edu.itm.prestamosdeportivos.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import co.edu.itm.prestamosdeportivos.aplicacion.UsuarioConsultaServicio;
import co.edu.itm.prestamosdeportivos.aplicacion.UsuarioRegistroServicio;
import co.edu.itm.prestamosdeportivos.configuracion.ConfiguracionReloj;
import co.edu.itm.prestamosdeportivos.configuracion.ConfiguracionSeguridad;
import co.edu.itm.prestamosdeportivos.configuracion.ManejadorErroresSeguridad;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.web.dto.UsuarioDto.SolicitanteRespuesta;
import co.edu.itm.prestamosdeportivos.web.error.DelegacionErroresSeguridad;
import co.edu.itm.prestamosdeportivos.web.error.ExcepcionApi;
import co.edu.itm.prestamosdeportivos.web.error.FabricaProblemas;
import co.edu.itm.prestamosdeportivos.web.error.ManejadorGlobalErrores;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsuarioControlador.class)
@Import({ConfiguracionSeguridad.class, ConfiguracionReloj.class,
        ManejadorErroresSeguridad.class, DelegacionErroresSeguridad.class, FabricaProblemas.class,
        ManejadorGlobalErrores.class})
class UsuarioControladorWebTest {

    @Autowired MockMvc mvc;
    @MockitoBean UsuarioConsultaServicio consulta;
    @MockitoBean UsuarioRegistroServicio registro;
    @MockitoBean UsuarioRepositorio usuarios;
    @MockitoBean JwtDecoder decodificadorJwt;

    @Test
    @DisplayName("PRESTADOR crea un solicitante y recibe Location")
    void prestadorCreaSolicitante() throws Exception {
        UUID id = UUID.randomUUID();
        when(registro.registrar(any())).thenReturn(new SolicitanteRespuesta(id, "Laura", "Gómez", "102334455", true));

        mvc.perform(post("/api/v1/usuarios").with(user("prestador").roles("PRESTADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"102334455\",\"nombres\":\"Laura\",\"apellidos\":\"Gómez\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/usuarios/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("ADMIN no puede crear solicitantes desde el flujo operativo")
    void adminNoCreaSolicitante() throws Exception {
        mvc.perform(post("/api/v1/usuarios").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"102334455\",\"nombres\":\"Laura\",\"apellidos\":\"Gómez\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        verifyNoInteractions(registro);
    }

    @Test
    @DisplayName("La validación impide crear solicitantes incompletos")
    void validaAntesDeDelegar() throws Exception {
        mvc.perform(post("/api/v1/usuarios").with(user("prestador").roles("PRESTADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"\",\"nombres\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"));
        verifyNoInteractions(registro);
    }

    @Test
    @DisplayName("La identificación duplicada se expone como conflicto comprensible")
    void identificaDuplicada() throws Exception {
        when(registro.registrar(any())).thenThrow(new ExcepcionApi(HttpStatus.CONFLICT,
                "CONFLICTO_UNICIDAD", "Registro duplicado", "Ya existe un usuario con esa identificación."));

        mvc.perform(post("/api/v1/usuarios").with(user("prestador").roles("PRESTADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"102334455\",\"nombres\":\"Laura\",\"apellidos\":\"Gómez\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"));
    }
}
