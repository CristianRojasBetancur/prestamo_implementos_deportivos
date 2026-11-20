package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.HistorialEstadoImplementoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.dto.IncidenciaDto.RegistrarIncidenciaSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IncidenciaServicioIntegracionTest {

    @Autowired private IncidenciaServicio incidenciaServicio;
    @Autowired private PrestamoServicio prestamoServicio;
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private CategoriaRepositorio categorias;
    @Autowired private ImplementoDeportivoRepositorio implementos;
    @Autowired private PrestamoRepositorio prestamos;
    @Autowired private IncidenciaRepositorio incidencias;
    @Autowired private HistorialEstadoImplementoRepositorio historial;
    @Autowired private MockMvc mvc;

    @ParameterizedTest
    @EnumSource(value = TipoIncidencia.class, names = {"PERDIDA", "ROBO"})
    @DisplayName("CU-18: pérdida y robo resuelven la unidad sin devolución")
    void registraPerdidaORobo(TipoIncidencia tipo) {
        var f = fixture();
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), List.of(f.unidad().getId()), f.prestador());
        long eventosIniciales = historial.count();

        var respuesta = incidenciaServicio.registrar(prestamo.id(),
                new RegistrarIncidenciaSolicitud(f.unidad().getId(), tipo, "La unidad no fue localizada."), f.prestador());

        assertThat(respuesta.tipo()).isEqualTo(tipo);
        assertThat(respuesta.estado()).isEqualTo(co.edu.itm.prestamosdeportivos.dominio.EstadoIncidencia.REGISTRADA);
        assertThat(implementos.findById(f.unidad().getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoImplemento.DADO_DE_BAJA);
        var detalle = prestamos.findById(prestamo.id()).orElseThrow().getDetalles().get(0);
        assertThat(detalle.getEstado()).isEqualTo(EstadoDetallePrestamo.RESUELTO_POR_INCIDENCIA);
        assertThat(prestamos.findById(prestamo.id()).orElseThrow().getEstado()).isEqualTo(EstadoPrestamo.FINALIZADO);
        assertThat(historial.count()).isEqualTo(eventosIniciales + 1);
    }

    @Test
    @DisplayName("CU-18: daño y otra novedad solo se registran dentro de una devolución")
    void rechazaTiposDeIncidenciaDevolucion() {
        var f = fixture();
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), List.of(f.unidad().getId()), f.prestador());
        long incidenciasIniciales = incidencias.count();

        assertThatThrownBy(() -> incidenciaServicio.registrar(prestamo.id(),
                new RegistrarIncidenciaSolicitud(f.unidad().getId(), TipoIncidencia.DANO, "Daño reportado"), f.prestador()))
                .isInstanceOf(ExcepcionDominio.class)
                .extracting(error -> ((ExcepcionDominio) error).getCodigo())
                .isEqualTo(CodigoError.TIPO_INCIDENCIA_NO_PERMITIDO);
        assertThat(incidencias.count()).isEqualTo(incidenciasIniciales);
        assertThat(implementos.findById(f.unidad().getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoImplemento.PRESTADO);
    }

    @Test
    @DisplayName("CU-18: una unidad ajena al préstamo se rechaza sin cambios")
    void unidadNoPerteneceAlPrestamo() {
        var f = fixture();
        var otra = crearUnidad(f.categoria(), "OTRA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), List.of(f.unidad().getId()), f.prestador());

        assertThatThrownBy(() -> incidenciaServicio.registrar(prestamo.id(),
                new RegistrarIncidenciaSolicitud(otra.getId(), TipoIncidencia.ROBO, "No aparece en inventario"), f.prestador()))
                .isInstanceOf(ExcepcionDominio.class)
                .extracting(error -> ((ExcepcionDominio) error).getCodigo())
                .isEqualTo(CodigoError.IMPLEMENTO_NO_PERTENECE_AL_PRESTAMO);
        assertThat(implementos.findById(otra.getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.DISPONIBLE);
    }

    @Test
    @DisplayName("CU-19: consulta filtra, pagina y expone el detalle sin credenciales")
    void consultaFiltradaYDetalle() throws Exception {
        var f = fixture();
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), List.of(f.unidad().getId()), f.prestador());
        var incidencia = incidenciaServicio.registrar(prestamo.id(),
                new RegistrarIncidenciaSolicitud(f.unidad().getId(), TipoIncidencia.PERDIDA, "Reporte de pérdida"), f.prestador());
        var auth = autenticacion(f.prestador(), "ROLE_PRESTADOR");

        mvc.perform(get("/api/v1/incidencias").param("tipo", "PERDIDA").param("tamano", "10").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(incidencia.id().toString()))
                .andExpect(jsonPath("$.contenido[0].tipo").value("PERDIDA"));
        mvc.perform(get("/api/v1/incidencias/" + incidencia.id()).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Reporte de pérdida"))
                .andExpect(jsonPath("$.prestador.id").value(f.prestadorUsuario().getId().toString()))
                .andExpect(jsonPath("$.prestador.correo").doesNotExist())
                .andExpect(jsonPath("$.prestador.contrasenaHash").doesNotExist());
    }

    @Test
    @DisplayName("Resolución administrativa audita la operación y no reactiva el inventario")
    void resuelveComoAdministrador() throws Exception {
        var f = fixture();
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), List.of(f.unidad().getId()), f.prestador());
        var incidencia = incidenciaServicio.registrar(prestamo.id(),
                new RegistrarIncidenciaSolicitud(f.unidad().getId(), TipoIncidencia.ROBO, "Reporte de robo"), f.prestador());

        var respuesta = incidenciaServicio.resolver(incidencia.id(), f.administrador());

        assertThat(respuesta.estado()).isEqualTo(co.edu.itm.prestamosdeportivos.dominio.EstadoIncidencia.RESUELTA);
        assertThat(respuesta.fechaResolucion()).isNotNull();
        assertThat(respuesta.resueltoPor().id()).isEqualTo(f.administradorUsuario().getId());
        assertThat(implementos.findById(f.unidad().getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoImplemento.DADO_DE_BAJA);
        assertThatThrownBy(() -> incidenciaServicio.resolver(incidencia.id(), f.administrador()))
                .isInstanceOf(ExcepcionDominio.class)
                .extracting(error -> ((ExcepcionDominio) error).getCodigo())
                .isEqualTo(CodigoError.INCIDENCIA_YA_RESUELTA);
    }

    @Test
    @DisplayName("Solo ADMIN puede resolver una incidencia")
    void resolucionRequiereAdministrador() {
        var f = fixture();
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), List.of(f.unidad().getId()), f.prestador());
        var incidencia = incidenciaServicio.registrar(prestamo.id(),
                new RegistrarIncidenciaSolicitud(f.unidad().getId(), TipoIncidencia.PERDIDA, "Reporte de pérdida"), f.prestador());

        assertThatThrownBy(() -> incidenciaServicio.resolver(incidencia.id(), f.prestador()))
                .isInstanceOf(ExcepcionDominio.class)
                .extracting(error -> ((ExcepcionDominio) error).getCodigo())
                .isEqualTo(CodigoError.ACCESO_DENEGADO);
    }

    private Fixture fixture() {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Usuario usuario = usuarios.saveAndFlush(Usuario.solicitante("u" + sufijo, "Usuario", "Prueba"));
        Usuario prestador = usuarios.saveAndFlush(Usuario.conAcceso("p" + sufijo, "Prestador", "Prueba",
                "p" + sufijo + "@itm.edu.co", "hash", Rol.PRESTADOR));
        Usuario administrador = usuarios.saveAndFlush(Usuario.conAcceso("a" + sufijo, "Administrador", "Prueba",
                "a" + sufijo + "@itm.edu.co", "hash", Rol.ADMIN));
        Categoria categoria = categorias.saveAndFlush(new Categoria("Cat " + sufijo, null));
        ImplementoDeportivo unidad = crearUnidad(categoria, "COD-" + sufijo);
        return new Fixture(usuario, prestador, administrador, categoria, unidad,
                new UsuarioAutenticado(prestador.getId(), Rol.PRESTADOR),
                new UsuarioAutenticado(administrador.getId(), Rol.ADMIN));
    }

    private ImplementoDeportivo crearUnidad(Categoria categoria, String codigo) {
        return implementos.saveAndFlush(new ImplementoDeportivo(codigo, "Unidad de prueba", null, categoria,
                EstadoImplemento.DISPONIBLE));
    }

    private UsernamePasswordAuthenticationToken autenticacion(UsuarioAutenticado principal, String autoridad) {
        return UsernamePasswordAuthenticationToken.authenticated(principal, "test",
                List.of(new SimpleGrantedAuthority(autoridad)));
    }

    private record Fixture(Usuario usuario, Usuario prestadorUsuario, Usuario administradorUsuario, Categoria categoria,
            ImplementoDeportivo unidad, UsuarioAutenticado prestador, UsuarioAutenticado administrador) {
    }
}
