package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
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
import co.edu.itm.prestamosdeportivos.persistencia.DevolucionRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.SancionRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.IncidenciaEnDevolucionSolicitud;
import co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.ItemDevolucionSolicitud;
import co.edu.itm.prestamosdeportivos.web.dto.DevolucionDto.RegistrarDevolucionSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevolucionServicioIntegracionTest {

    @Autowired private DevolucionServicio devolucionServicio;
    @Autowired private PrestamoServicio prestamoServicio;
    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private CategoriaRepositorio categorias;
    @Autowired private ImplementoDeportivoRepositorio implementos;
    @Autowired private PrestamoRepositorio prestamos;
    @Autowired private DevolucionRepositorio devoluciones;
    @Autowired private IncidenciaRepositorio incidencias;
    @Autowired private SancionRepositorio sanciones;
    @Autowired private TransactionTemplate transacciones;
    @Autowired private jakarta.persistence.EntityManager entityManager;
    @Autowired private MockMvc mvc;

    @Test
    @DisplayName("CU-16: devolución parcial solo resuelve las unidades recibidas")
    void devolucionParcialMantienePrestamoAbierto() {
        var f = fixture(2);
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), f.unidades().stream().map(ImplementoDeportivo::getId).toList(), f.operador());
        var respuesta = devolucionServicio.registrar(prestamo.id(), solicitud(
                item(f.unidades().get(0), CondicionDevolucion.BUENO, null)), f.operador());

        assertThat(respuesta.alcance()).isEqualTo(co.edu.itm.prestamosdeportivos.dominio.AlcanceDevolucion.PARCIAL);
        assertThat(prestamos.findById(prestamo.id()).orElseThrow().getEstado()).isEqualTo(EstadoPrestamo.DEVOLUCION_PARCIAL);
        assertThat(implementos.findById(f.unidades().get(0).getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.DISPONIBLE);
        assertThat(implementos.findById(f.unidades().get(1).getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.PRESTADO);
        assertThat(prestamos.findById(prestamo.id()).orElseThrow().getDetalles()).extracting(d -> d.getEstado())
                .containsExactlyInAnyOrder(EstadoDetallePrestamo.DEVUELTO, EstadoDetallePrestamo.PENDIENTE_DEVOLUCION);
    }

    @Test
    @DisplayName("CU-16/RN-03: devolución completa con novedad cierra, mantiene la unidad fuera de disponibilidad y sanciona el retraso")
    void devolucionCompletaConNovedadYSancion() {
        var f = fixture(2);
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), f.unidades().stream().map(ImplementoDeportivo::getId).toList(), f.operador());
        Instant limite = Instant.now().minusSeconds(3600);
        Instant inicio = limite.minusSeconds(1);
        transacciones.executeWithoutResult(status -> entityManager.createNativeQuery(
                "update prestamo set fecha_prestamo = :inicio, fecha_limite_devolucion = :limite where id = :id")
                .setParameter("inicio", inicio).setParameter("limite", limite).setParameter("id", prestamo.id()).executeUpdate());
        entityManager.clear();

        var respuesta = devolucionServicio.registrar(prestamo.id(), new RegistrarDevolucionSolicitud(List.of(
                item(f.unidades().get(0), CondicionDevolucion.BUENO, null),
                item(f.unidades().get(1), CondicionDevolucion.CON_NOVEDAD,
                        new IncidenciaEnDevolucionSolicitud(TipoIncidencia.DANO, "Costura rota"))),
                false, null, "Recibido en ventanilla"), f.operador());

        assertThat(respuesta.alcance()).isEqualTo(co.edu.itm.prestamosdeportivos.dominio.AlcanceDevolucion.COMPLETA);
        assertThat(respuesta.sancion()).isNotNull();
        assertThat(prestamos.findById(prestamo.id()).orElseThrow().getEstado()).isEqualTo(EstadoPrestamo.FINALIZADO);
        assertThat(implementos.findById(f.unidades().get(0).getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.DISPONIBLE);
        assertThat(implementos.findById(f.unidades().get(1).getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.EN_MANTENIMIENTO);
        assertThat(incidencias.findByPrestamoIdOrderByFechaRegistroDesc(prestamo.id())).hasSize(1);
        assertThat(sanciones.findByDevolucionId(respuesta.id())).isPresent();
    }

    @Test
    @DisplayName("CU-16: una incidencia requerida falla sin dejar cambios parciales")
    void novedadSinIncidenciaRevierte() {
        var f = fixture(1);
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), List.of(f.unidades().get(0).getId()), f.operador());
        long devolucionesIniciales = devoluciones.count();

        assertThatThrownBy(() -> devolucionServicio.registrar(prestamo.id(), solicitud(
                item(f.unidades().get(0), CondicionDevolucion.CON_NOVEDAD, null)), f.operador()))
                .isInstanceOf(ExcepcionDominio.class)
                .extracting(error -> ((ExcepcionDominio) error).getCodigo())
                .isEqualTo(CodigoError.INCIDENCIA_REQUERIDA);
        assertThat(devoluciones.count()).isEqualTo(devolucionesIniciales);
        assertThat(implementos.findById(f.unidades().get(0).getId()).orElseThrow().getEstado()).isEqualTo(EstadoImplemento.PRESTADO);
        assertThat(prestamos.findById(prestamo.id()).orElseThrow().getEstado()).isEqualTo(EstadoPrestamo.VIGENTE);
    }

    @Test
    @DisplayName("CU-17: consulta paginada filtra por alcance y expone detalle completo")
    void consultaPaginadaYDetalle() throws Exception {
        var f = fixture(1);
        var prestamo = prestamoServicio.registrar(f.usuario().getId(), List.of(f.unidades().get(0).getId()), f.operador());
        var devolucion = devolucionServicio.registrar(prestamo.id(), solicitud(
                item(f.unidades().get(0), CondicionDevolucion.BUENO, null)), f.operador());
        var auth = UsernamePasswordAuthenticationToken.authenticated(f.operador(), "test",
                List.of(new SimpleGrantedAuthority("ROLE_PRESTADOR")));

        mvc.perform(get("/api/v1/devoluciones").param("prestamoId", prestamo.id().toString())
                .param("alcance", "COMPLETA").param("tamano", "10")
                .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(devolucion.id().toString()))
                .andExpect(jsonPath("$.contenido[0].prestamo.id").value(prestamo.id().toString()))
                .andExpect(jsonPath("$.contenido[0].implementos.length()").value(1));
        mvc.perform(get("/api/v1/devoluciones/" + devolucion.id()).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].condicion").value("BUENO"));
    }

    private RegistrarDevolucionSolicitud solicitud(ItemDevolucionSolicitud... items) {
        return new RegistrarDevolucionSolicitud(List.of(items), false, null, null);
    }

    private ItemDevolucionSolicitud item(ImplementoDeportivo unidad, CondicionDevolucion condicion,
            IncidenciaEnDevolucionSolicitud incidencia) {
        return new ItemDevolucionSolicitud(unidad.getId(), condicion, null, incidencia);
    }

    private Fixture fixture(int cantidad) {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Usuario usuario = usuarios.saveAndFlush(Usuario.solicitante("u" + sufijo, "Usuario", "Prueba"));
        Usuario prestador = usuarios.saveAndFlush(Usuario.conAcceso("p" + sufijo, "Prestador", "Prueba",
                "p" + sufijo + "@itm.edu.co", "hash", Rol.PRESTADOR));
        Categoria categoria = categorias.saveAndFlush(new Categoria("Cat " + sufijo, null));
        List<ImplementoDeportivo> unidades = java.util.stream.IntStream.range(0, cantidad)
                .mapToObj(i -> implementos.saveAndFlush(new ImplementoDeportivo("COD-" + sufijo + i,
                        "Unidad de prueba", null, categoria, EstadoImplemento.DISPONIBLE)))
                .toList();
        return new Fixture(usuario, unidades, new UsuarioAutenticado(prestador.getId(), Rol.PRESTADOR));
    }

    private record Fixture(Usuario usuario, List<ImplementoDeportivo> unidades, UsuarioAutenticado operador) {
    }
}
