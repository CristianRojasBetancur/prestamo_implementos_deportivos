package co.edu.itm.prestamosdeportivos.web.error;

import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.LISTA_NO_VACIA;
import static co.edu.itm.prestamosdeportivos.web.dto.MensajesValidacion.OBLIGATORIO;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.configuracion.FiltroCorrelacion;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

/**
 * Pruebas MVC del contrato de errores (anexo A de BE-01) con un controlador de
 * prueba que dispara cada categoría de error. Incluye la cadena real de Spring
 * Security, el filtro de correlación y el manejador global.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({ContenedoresPrueba.class, ContratoErroresMvcTest.ControladorPrueba.class})
class ContratoErroresMvcTest {

    private static final String BASE = "/api/v1/pruebas-contrato";
    private static final String UUID_VALIDO = "7e8f9a0b-1c2d-4e3f-a4b5-c6d7e8f9a0b1";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private FiltroCorrelacion filtroCorrelacion;

    private MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .addFilters(filtroCorrelacion)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---------------------------------------------------------------- 400

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("400: campos inválidos devuelven errores por campo legibles")
    void validacionPorCampo() throws Exception {
        mvc.perform(post(BASE + "/validacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\", \"ids\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value(BASE + "/validacion"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')].mensaje").value(hasItem(OBLIGATORIO)))
                .andExpect(jsonPath("$.errores[?(@.campo == 'ids')].mensaje").value(hasItem(LISTA_NO_VACIA)));
    }

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("400: JSON mal formado devuelve SOLICITUD_INVALIDA sin detalles del parser")
    void jsonMalFormado() throws Exception {
        mvc.perform(post(BASE + "/validacion").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.detail").value(not(containsString("Jackson"))));
    }

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("400: una fecha sin zona horaria se rechaza")
    void fechaSinZona() throws Exception {
        mvc.perform(post(BASE + "/validacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"x\", \"ids\": [\"" + UUID_VALIDO + "\"], \"fecha\": \"2026-08-31T18:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("400: un UUID mal formado en la ruta indica el parámetro")
    void uuidInvalido() throws Exception {
        mvc.perform(get(BASE + "/recurso/no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.errores[0].campo").value("id"));
    }

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("400: paginación fuera de rango y orden no permitido")
    void paginacionInvalida() throws Exception {
        mvc.perform(get(BASE + "/pagina").param("tamano", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("tamano"));
        mvc.perform(get(BASE + "/pagina").param("orden", "contrasenaHash,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("orden"));
    }

    // ---------------------------------------------------------- 401 / 403

    @Test
    @DisplayName("401: sin autenticación, con el formato común y correlación")
    void sinAutenticacion() throws Exception {
        mvc.perform(get(BASE + "/recurso/" + UUID_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(header().exists(FiltroCorrelacion.CABECERA));
    }

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("403: rol insuficiente")
    void rolInsuficiente() throws Exception {
        mvc.perform(get(BASE + "/solo-admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    // ---------------------------------------------------------------- 404

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("404: recurso inexistente (excepción de dominio) y ruta inexistente")
    void noEncontrado() throws Exception {
        mvc.perform(get(BASE + "/recurso/" + UUID_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.detail").value("El préstamo no existe."));
        mvc.perform(get("/api/v1/ruta-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
    }

    // ------------------------------------------------------------ 405

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("405: método no permitido")
    void metodoNoPermitido() throws Exception {
        mvc.perform(post(BASE + "/conflicto"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
    }

    // ---------------------------------------------------------------- 409

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("409: conflicto de disponibilidad y duplicidad sin filtrar SQL")
    void conflictos() throws Exception {
        mvc.perform(get(BASE + "/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("IMPLEMENTO_NO_DISPONIBLE"));
        mvc.perform(get(BASE + "/duplicado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_UNICIDAD"))
                .andExpect(content().string(not(containsString("uq_usuario_correo"))))
                .andExpect(content().string(not(containsString("INSERT"))));
    }

    // ---------------------------------------------------------------- 422

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("422: regla de negocio no cumplida")
    void reglaDeNegocio() throws Exception {
        mvc.perform(get(BASE + "/regla"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("LIMITE_IMPLEMENTOS_EXCEDIDO"));
    }

    // ---------------------------------------------------------- 500 / 503

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("500: error interno sin trazas ni datos sensibles, con correlación del cliente")
    void errorInterno() throws Exception {
        mvc.perform(get(BASE + "/interno").header(FiltroCorrelacion.CABECERA, "front-abc-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.correlacionId").value("front-abc-123"))
                .andExpect(header().string(FiltroCorrelacion.CABECERA, "front-abc-123"))
                .andExpect(content().string(not(containsString("contrasena"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at co.edu"))));
    }

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("503: base de datos no disponible")
    void dependenciaNoDisponible() throws Exception {
        mvc.perform(get(BASE + "/bd-caida"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SERVICIO_NO_DISPONIBLE"));
    }

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("Una cabecera de correlación insegura se reemplaza por un UUID generado")
    void correlacionInsegura() throws Exception {
        mvc.perform(get(BASE + "/regla").header(FiltroCorrelacion.CABECERA, "<script>alert(1)</script>"))
                .andExpect(header().string(FiltroCorrelacion.CABECERA, matchesPattern("[0-9a-f-]{36}")));
    }

    // ------------------------------------------------------- serialización

    @Test
    @WithMockUser(roles = "PRESTADOR")
    @DisplayName("Las fechas se serializan en ISO-8601 con zona y los enums por nombre")
    void serializacionFechasYEnums() throws Exception {
        mvc.perform(get(BASE + "/fecha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-08-31T18:00:00-05:00"))
                .andExpect(jsonPath("$.estado").value("VIGENTE"));
    }

    /** Controlador solo para pruebas; no se registra en la aplicación real. */
    @RestController
    @RequestMapping(BASE)
    public static class ControladorPrueba {

        public record SolicitudPrueba(@NotBlank(message = OBLIGATORIO) String nombre,
                @NotEmpty(message = LISTA_NO_VACIA) List<UUID> ids, OffsetDateTime fecha) {
        }

        public record RespuestaPrueba(OffsetDateTime fecha, EstadoPrestamo estado) {
        }

        private final ConversorFechas conversorFechas;

        public ControladorPrueba(ConversorFechas conversorFechas) {
            this.conversorFechas = conversorFechas;
        }

        @PostMapping("/validacion")
        public RespuestaPrueba validar(@Valid @RequestBody SolicitudPrueba solicitud) {
            return new RespuestaPrueba(solicitud.fecha(), EstadoPrestamo.VIGENTE);
        }

        @GetMapping("/recurso/{id}")
        public String recurso(@PathVariable UUID id) {
            throw new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO, "El préstamo no existe.");
        }

        @GetMapping("/pagina")
        public String pagina(@Valid ParametrosPagina pagina) {
            pagina.aPageable(Set.of("fechaPrestamo"), Sort.by("fechaPrestamo"));
            return "ok";
        }

        @GetMapping("/solo-admin")
        @PreAuthorize("hasRole('ADMIN')")
        public String soloAdmin() {
            return "ok";
        }

        @GetMapping("/conflicto")
        public String conflicto() {
            throw new ExcepcionDominio(CodigoError.IMPLEMENTO_NO_DISPONIBLE, "El implemento BAL-001 no está disponible.");
        }

        @GetMapping("/duplicado")
        public String duplicado() {
            throw new DataIntegrityViolationException("INSERT INTO usuario ... uq_usuario_correo",
                    new SQLException("duplicate key value violates unique constraint \"uq_usuario_correo\"", "23505"));
        }

        @GetMapping("/regla")
        public String regla() {
            throw new ExcepcionDominio(CodigoError.LIMITE_IMPLEMENTOS_EXCEDIDO,
                    "El usuario tiene 3 implementos activos y solo puede recibir 1 más (máximo 4).");
        }

        @GetMapping("/interno")
        public String interno() {
            throw new IllegalStateException("Fallo leyendo contrasena_hash de la tabla usuario");
        }

        @GetMapping("/bd-caida")
        public String bdCaida() {
            throw new DataAccessResourceFailureException("Connection refused: localhost:5432");
        }

        @GetMapping("/fecha")
        public RespuestaPrueba fecha() {
            return new RespuestaPrueba(conversorFechas.aRespuesta(Instant.parse("2026-08-31T23:00:00Z")),
                    EstadoPrestamo.VIGENTE);
        }
    }
}
