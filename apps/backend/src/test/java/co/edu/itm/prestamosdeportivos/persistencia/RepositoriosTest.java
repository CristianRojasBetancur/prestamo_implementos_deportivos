package co.edu.itm.prestamosdeportivos.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import co.edu.itm.prestamosdeportivos.dominio.AlcanceDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.DetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.Devolucion;
import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EventoEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.HistorialEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Incidencia;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Sancion;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.aplicacion.InventarioServicio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistencia y consultas mínimas de los repositorios sobre PostgreSQL real.
 * Que el contexto levante confirma además que el mapeo JPA coincide con el
 * esquema de Flyway ({@code ddl-auto: validate}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@Transactional
class RepositoriosTest {

    private static final Instant AHORA = Instant.parse("2026-08-31T23:00:00Z");

    @Autowired private UsuarioRepositorio usuarios;
    @Autowired private CategoriaRepositorio categorias;
    @Autowired private ImplementoDeportivoRepositorio implementos;
    @Autowired private DisponibilidadRepositorio disponibilidades;
    @Autowired private HistorialEstadoImplementoRepositorio historialEstados;
    @Autowired private InventarioServicio inventarioServicio;
    @Autowired private PrestamoRepositorio prestamos;
    @Autowired private DetallePrestamoRepositorio detallesPrestamo;
    @Autowired private DevolucionRepositorio devoluciones;
    @Autowired private IncidenciaRepositorio incidencias;
    @Autowired private SancionRepositorio sanciones;
    @Autowired private EntityManager entityManager;

    private Usuario solicitante;
    private Usuario prestador;
    private ImplementoDeportivo balon;
    private ImplementoDeportivo peto;
    private Prestamo prestamo;

    @BeforeEach
    void datos() {
        solicitante = usuarios.save(Usuario.solicitante("102334455", "Laura", "Gómez"));
        prestador = usuarios.save(Usuario.conAcceso("1012345678", "María", "Rodríguez",
                "maria.rodriguez@institucion.edu.co", "$2a$10$hashDePrueba", Rol.PRESTADOR));
        Categoria balones = categorias.save(new Categoria("Balones", null));
        balon = implementos.save(new ImplementoDeportivo("BAL-001", "Balón de fútbol", null, balones,
                EstadoImplemento.DISPONIBLE));
        peto = implementos.save(new ImplementoDeportivo("PET-014", "Petos deportivos", null, balones,
                EstadoImplemento.DISPONIBLE));

        prestamo = new Prestamo(solicitante, prestador, AHORA, AHORA.plus(Duration.ofHours(24)));
        prestamo.agregarDetalle(balon);
        prestamo.agregarDetalle(peto);
        prestamo = prestamos.saveAndFlush(prestamo);
    }

    @Test
    @DisplayName("Usuarios: búsqueda por identificación, correo sin mayúsculas y rol")
    void usuarios() {
        assertThat(usuarios.findByIdentificacion("102334455")).contains(solicitante);
        assertThat(usuarios.buscarPorCorreo("MARIA.RODRIGUEZ@institucion.edu.co")).contains(prestador);
        assertThat(usuarios.existeCorreo("otro@institucion.edu.co")).isFalse();
        assertThat(usuarios.findByIdAndRol(prestador.getId(), Rol.PRESTADOR)).isPresent();
        assertThat(usuarios.findByIdAndRol(solicitante.getId(), Rol.PRESTADOR)).isEmpty();
        assertThat(solicitante.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("Categorías: búsqueda por nombre sin mayúsculas y listado de activas")
    void categorias() {
        assertThat(categorias.buscarPorNombre("BALONES")).isPresent();
        assertThat(categorias.findByActivaTrueOrderByNombreAsc()).extracting(Categoria::getNombre).contains("Balones");
    }

    @Test
    @DisplayName("Unidades: búsqueda por código, conteo por estado y bloqueo pesimista")
    void implementos() {
        assertThat(implementos.findByCodigoInventario("BAL-001")).contains(balon);
        assertThat(implementos.countByEstado(EstadoImplemento.DISPONIBLE)).isGreaterThanOrEqualTo(2);

        List<ImplementoDeportivo> bloqueados = implementos.bloquearPorIds(List.of(peto.getId(), balon.getId()));
        assertThat(bloqueados).containsExactlyInAnyOrder(balon, peto);
    }

    @Test
    @DisplayName("Inventario: q ignora tildes, aplica filtros y mantiene historial de cambios de estado")
    void filtrosYHistorialDeInventario() {
        var pagina = inventarioServicio.listar("balón", balon.getCategoria().getId(), EstadoImplemento.DISPONIBLE,
                org.springframework.data.domain.PageRequest.of(0, 1));
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent()).containsExactly(balon);

        EstadoImplemento anterior = balon.getEstado();
        balon.cambiarEstado(EstadoImplemento.EN_MANTENIMIENTO, EventoEstadoImplemento.ADMINISTRACION);
        historialEstados.saveAndFlush(new HistorialEstadoImplemento(balon, anterior, EstadoImplemento.EN_MANTENIMIENTO,
                EventoEstadoImplemento.ADMINISTRACION, "Desgaste"));
        entityManager.clear();

        assertThat(historialEstados.findAll()).anySatisfy(registro -> {
            assertThat(registro.getEstadoAnterior()).isEqualTo(EstadoImplemento.DISPONIBLE);
            assertThat(registro.getEstadoNuevo()).isEqualTo(EstadoImplemento.EN_MANTENIMIENTO);
            assertThat(registro.getMotivo()).isEqualTo("Desgaste");
        });
    }

    @Test
    @DisplayName("Disponibilidad: agrega por categoría/nombre, filtra y excluye grupos sin unidades elegibles")
    void disponibilidadAgregadaYFiltrada() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("nombre").ascending()
                        .and(org.springframework.data.domain.Sort.by("categoriaNombre").ascending()));

        var todos = disponibilidades.buscarDisponibles(null, null, pageable);
        assertThat(todos.getTotalElements()).isEqualTo(2);
        assertThat(todos.getContent()).extracting(DisponibilidadProyeccion::getNombre)
                .containsExactly("Balón de fútbol", "Petos deportivos");

        var primeraPagina = disponibilidades.buscarDisponibles(null, null,
                org.springframework.data.domain.PageRequest.of(0, 1, pageable.getSort()));
        assertThat(primeraPagina.getTotalPages()).isEqualTo(2);
        assertThat(primeraPagina.getContent()).hasSize(1);

        var porCategoriaYTexto = disponibilidades.buscarDisponibles("fútbol", balon.getCategoria().getId(), pageable);
        assertThat(porCategoriaYTexto.getTotalElements()).isEqualTo(1);
        assertThat(porCategoriaYTexto.getContent().getFirst().getUnidadesDisponibles()).isEqualTo(1);

        DetallePrestamo detallePeto = detallesPrestamo
                .findByPrestamoIdAndEstado(prestamo.getId(), EstadoDetallePrestamo.PENDIENTE_DEVOLUCION).stream()
                .filter(detalle -> detalle.getImplemento().equals(peto))
                .findFirst().orElseThrow();
        incidencias.saveAndFlush(new Incidencia(detallePeto, prestador, TipoIncidencia.OTRA,
                "Novedad pendiente de resolución", AHORA));

        var conIncidenciaAbierta = disponibilidades.buscarDisponibles(null, balon.getCategoria().getId(), pageable);
        assertThat(conIncidenciaAbierta.getTotalElements()).isEqualTo(1);
        assertThat(conIncidenciaAbierta.getContent()).extracting(DisponibilidadProyeccion::getNombre)
                .containsExactly("Balón de fútbol");

        balon.cambiarEstado(EstadoImplemento.EN_MANTENIMIENTO, EventoEstadoImplemento.ADMINISTRACION);
        implementos.saveAndFlush(balon);
        assertThat(disponibilidades.buscarDisponibles(null, balon.getCategoria().getId(), pageable).getContent()).isEmpty();
    }

    @Test
    @DisplayName("Disponibilidad: préstamo activo y baja no generan disponibilidad pública")
    void disponibilidadExcluyePrestamoActivoYBaja() {
        entityManager.createNativeQuery("UPDATE implemento_deportivo SET estado = 'PRESTADO' WHERE id = :id")
                .setParameter("id", balon.getId()).executeUpdate();
        entityManager.createNativeQuery("UPDATE implemento_deportivo SET estado = 'DADO_DE_BAJA' WHERE id = :id")
                .setParameter("id", peto.getId()).executeUpdate();

        var pagina = disponibilidades.buscarDisponibles(null, balon.getCategoria().getId(),
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Préstamos: código generado, detalles en cascada, unidades pendientes y vencimiento")
    void prestamos() {
        assertThat(prestamo.getCodigo()).matches("PRE-\\d{6}");
        assertThat(prestamo.getEstado()).isEqualTo(EstadoPrestamo.VIGENTE);
        assertThat(prestamos.findByCodigo(prestamo.getCodigo())).contains(prestamo);

        assertThat(detallesPrestamo.contarUnidadesPendientesDeUsuario(solicitante.getId())).isEqualTo(2);
        assertThat(detallesPrestamo.existsByImplementoIdAndEstado(balon.getId(),
                EstadoDetallePrestamo.PENDIENTE_DEVOLUCION)).isTrue();
        assertThat(prestamos.countByUsuarioIdAndEstadoNot(solicitante.getId(), EstadoPrestamo.FINALIZADO)).isEqualTo(1);

        assertThat(prestamos.tienePrestamosVencidos(solicitante.getId(), AHORA.plus(Duration.ofHours(23)))).isFalse();
        assertThat(prestamos.tienePrestamosVencidos(solicitante.getId(), AHORA.plus(Duration.ofHours(25)))).isTrue();
    }

    @Test
    @DisplayName("Devoluciones, incidencias y sanciones se persisten y consultan")
    void devolucionIncidenciaYSancion() {
        DetallePrestamo detallePeto = detallesPrestamo
                .findByPrestamoIdAndEstado(prestamo.getId(), EstadoDetallePrestamo.PENDIENTE_DEVOLUCION).stream()
                .filter(detalle -> detalle.getImplemento().equals(peto))
                .findFirst().orElseThrow();
        Instant fechaDevolucion = AHORA.plus(Duration.ofHours(26));

        Incidencia incidencia = incidencias.saveAndFlush(new Incidencia(detallePeto, prestador, TipoIncidencia.DANO,
                "Peto con costura lateral rota.", fechaDevolucion));
        Devolucion devolucion = new Devolucion(prestamo, prestador, fechaDevolucion, AlcanceDevolucion.PARCIAL,
                false, null, "Entrega en ventanilla");
        devolucion.agregarDetalle(detallePeto, CondicionDevolucion.CON_NOVEDAD, "Costura rota", incidencia);
        devolucion = devoluciones.saveAndFlush(devolucion);
        sanciones.saveAndFlush(new Sancion(solicitante, prestamo, devolucion, "Devolución tardía sin justificación",
                120, fechaDevolucion, fechaDevolucion.plus(2, ChronoUnit.DAYS), null));
        entityManager.clear();

        assertThat(incidencia.getCodigo()).matches("INC-\\d{6}");
        assertThat(devolucion.getCodigo()).matches("DEV-\\d{6}");
        assertThat(incidencias.findByPrestamoIdOrderByFechaRegistroDesc(prestamo.getId())).hasSize(1);
        assertThat(incidencias.findByImplementoIdOrderByFechaRegistroDesc(peto.getId())).hasSize(1);
        assertThat(devoluciones.findByPrestamoIdOrderByFechaDevolucionDesc(prestamo.getId()))
                .singleElement()
                .satisfies(registrada -> assertThat(registrada.getDetalles()).singleElement()
                        .satisfies(detalle -> assertThat(detalle.getIncidencia()).isNotNull()));

        assertThat(sanciones.buscarVigente(solicitante.getId(), fechaDevolucion.plus(1, ChronoUnit.DAYS))).isPresent();
        assertThat(sanciones.buscarVigente(solicitante.getId(), fechaDevolucion.plus(3, ChronoUnit.DAYS))).isEmpty();
    }
}
