package co.edu.itm.prestamosdeportivos.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import co.edu.itm.prestamosdeportivos.ContenedoresPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica sobre PostgreSQL real (Testcontainers) que la migración V2 crea las
 * restricciones de integridad e índices documentados en modelo-datos.md. Cada
 * prueba corre en una transacción que se revierte al terminar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@Transactional
class EsquemaPersistenciaTest {

    static final String USUARIO = "00000000-0000-0000-0000-000000000001";
    static final String PRESTADOR = "00000000-0000-0000-0000-000000000002";
    static final String CATEGORIA = "00000000-0000-0000-0000-0000000000c1";
    static final String IMPLEMENTO = "00000000-0000-0000-0000-0000000000a1";
    static final String PRESTAMO_1 = "00000000-0000-0000-0000-0000000000b1";
    static final String PRESTAMO_2 = "00000000-0000-0000-0000-0000000000b2";
    static final String DETALLE_1 = "00000000-0000-0000-0000-0000000000d1";

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void datosBase() {
        jdbc.update("INSERT INTO usuario (id, identificacion, nombres, apellidos, rol) "
                + "VALUES (?::uuid, '100', 'Laura', 'Gómez', 'USER')", USUARIO);
        jdbc.update("INSERT INTO usuario (id, identificacion, nombres, apellidos, correo, contrasena_hash, rol) "
                + "VALUES (?::uuid, '200', 'María', 'Rodríguez', 'maria@itm.edu.co', 'hash', 'PRESTADOR')", PRESTADOR);
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?::uuid, 'Balones')", CATEGORIA);
        jdbc.update("INSERT INTO implemento_deportivo (id, codigo_inventario, nombre, categoria_id) "
                + "VALUES (?::uuid, 'BAL-001', 'Balón de fútbol', ?::uuid)", IMPLEMENTO, CATEGORIA);
        for (String prestamo : List.of(PRESTAMO_1, PRESTAMO_2)) {
            jdbc.update("INSERT INTO prestamo (id, usuario_id, prestador_id, fecha_prestamo, fecha_limite_devolucion) "
                    + "VALUES (?::uuid, ?::uuid, ?::uuid, now(), now() + interval '24 hours')",
                    prestamo, USUARIO, PRESTADOR);
        }
        jdbc.update("INSERT INTO detalle_prestamo (id, prestamo_id, implemento_id) VALUES (?::uuid, ?::uuid, ?::uuid)",
                DETALLE_1, PRESTAMO_1, IMPLEMENTO);
    }

    @Test
    @DisplayName("Las migraciones V1 y V2 quedan aplicadas y crean las nueve tablas del MVP")
    void migracionesAplicadas() {
        List<String> versiones = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        assertThat(versiones).containsSubsequence("1", "2", "7");

        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name <> 'flyway_schema_history'", String.class);
        assertThat(tablas).containsExactlyInAnyOrder("usuario", "categoria", "implemento_deportivo", "prestamo",
                "detalle_prestamo", "devolucion", "detalle_devolucion", "incidencia", "sancion",
                "historial_estado_implemento");
    }

    @Test
    @DisplayName("El código legible del préstamo lo genera PostgreSQL con el formato PRE-000000")
    void codigoGenerado() {
        String codigo = jdbc.queryForObject("SELECT codigo FROM prestamo WHERE id = ?::uuid", String.class, PRESTAMO_1);
        assertThat(codigo).matches("PRE-\\d{6}");
    }

    @Nested
    @DisplayName("Integridad")
    class Integridad {

        @Test
        @DisplayName("RN-06: una unidad no puede quedar pendiente en dos préstamos")
        void unidadEnDosPrestamosActivos() {
            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO detalle_prestamo (prestamo_id, implemento_id) VALUES (?::uuid, ?::uuid)",
                    PRESTAMO_2, IMPLEMENTO))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uq_detalle_prestamo_implemento_pendiente");
        }

        @Test
        @DisplayName("RN-06: la unidad puede volver a prestarse cuando su detalle anterior se resuelve")
        void unidadReutilizableTrasResolverse() {
            jdbc.update("UPDATE detalle_prestamo SET estado = 'DEVUELTO', fecha_resolucion = now(), "
                    + "condicion_recibida = 'BUENO' WHERE id = ?::uuid", DETALLE_1);

            int filas = jdbc.update("INSERT INTO detalle_prestamo (prestamo_id, implemento_id) VALUES (?::uuid, ?::uuid)",
                    PRESTAMO_2, IMPLEMENTO);

            assertThat(filas).isEqualTo(1);
        }

        @Test
        @DisplayName("Las relaciones no pueden apuntar a registros inexistentes")
        void claveForaneaInexistente() {
            assertThatThrownBy(() -> jdbc.update(
                    "INSERT INTO detalle_prestamo (prestamo_id, implemento_id) VALUES (?::uuid, gen_random_uuid())",
                    PRESTAMO_2))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("fk_detalle_prestamo_implemento");
        }

        @Test
        @DisplayName("El historial no se borra: una categoría con unidades no puede eliminarse")
        void historialNoSeBorra() {
            assertThatThrownBy(() -> jdbc.update("DELETE FROM categoria WHERE id = ?::uuid", CATEGORIA))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("fk_implemento_categoria");
        }

        @Test
        @DisplayName("El código de inventario es único")
        void codigoInventarioUnico() {
            assertThatThrownBy(() -> jdbc.update("INSERT INTO implemento_deportivo (codigo_inventario, nombre, categoria_id) "
                    + "VALUES ('BAL-001', 'Otro', ?::uuid)", CATEGORIA))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uq_implemento_codigo_inventario");
        }

        @Test
        @DisplayName("El correo es único sin distinguir mayúsculas")
        void correoUnicoSinMayusculas() {
            assertThatThrownBy(() -> jdbc.update("INSERT INTO usuario (identificacion, nombres, apellidos, correo, "
                    + "contrasena_hash, rol) VALUES ('300', 'A', 'B', 'MARIA@ITM.EDU.CO', 'hash', 'ADMIN')"))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uq_usuario_correo");
        }

        @Test
        @DisplayName("Un PRESTADOR exige correo y contraseña")
        void prestadorSinCredenciales() {
            assertThatThrownBy(() -> jdbc.update("INSERT INTO usuario (identificacion, nombres, apellidos, rol) "
                    + "VALUES ('301', 'A', 'B', 'PRESTADOR')"))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_usuario_credenciales");
        }

        @Test
        @DisplayName("Los estados solo admiten los valores de la enumeración")
        void estadoInvalido() {
            assertThatThrownBy(() -> jdbc.update("UPDATE implemento_deportivo SET estado = 'ROTO'"))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_implemento_estado");
        }

        @Test
        @DisplayName("Los campos obligatorios son NOT NULL")
        void camposObligatorios() {
            assertThatThrownBy(() -> jdbc.update("INSERT INTO usuario (identificacion, apellidos, rol) "
                    + "VALUES ('302', 'B', 'USER')"))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("nombres");
        }

        @Test
        @DisplayName("Un préstamo finalizado exige fecha de finalización")
        void finalizadoSinFecha() {
            assertThatThrownBy(() -> jdbc.update("UPDATE prestamo SET estado = 'FINALIZADO' WHERE id = ?::uuid",
                    PRESTAMO_1))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_prestamo_finalizacion");
        }

        @Test
        @DisplayName("Un detalle devuelto exige fecha de resolución y condición")
        void detalleDevueltoIncompleto() {
            assertThatThrownBy(() -> jdbc.update("UPDATE detalle_prestamo SET estado = 'DEVUELTO', "
                    + "fecha_resolucion = now() WHERE id = ?::uuid", DETALLE_1))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_detalle_prestamo_resolucion");
        }

        @Test
        @DisplayName("Una incidencia no puede referenciar un préstamo distinto al de su detalle")
        void incidenciaIncoherente() {
            assertThatThrownBy(() -> jdbc.update("INSERT INTO incidencia (detalle_prestamo_id, prestamo_id, "
                    + "implemento_id, prestador_id, tipo, descripcion, fecha_registro) "
                    + "VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, 'DANO', 'Costura rota', now())",
                    DETALLE_1, PRESTAMO_2, IMPLEMENTO, PRESTADOR))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("fk_incidencia_detalle_prestamo");
        }
    }

    @Nested
    @DisplayName("Planes de consulta")
    class PlanesDeConsulta {

        @Test
        @DisplayName("Los filtros principales pueden resolverse con los índices documentados")
        void filtrosUsanIndices() {
            jdbc.execute("SET LOCAL enable_seqscan = off");

            assertThat(plan("SELECT id FROM prestamo WHERE usuario_id = '" + USUARIO + "' AND estado <> 'FINALIZADO'"))
                    .contains("ix_prestamo_usuario_estado");
            assertThat(plan("SELECT id FROM prestamo WHERE estado <> 'FINALIZADO' AND fecha_limite_devolucion < now()"))
                    .contains("ix_prestamo_vencimiento");
            assertThat(plan("SELECT id FROM usuario WHERE lower(correo) = 'maria@itm.edu.co'"))
                    .contains("uq_usuario_correo");
            assertThat(plan("SELECT id FROM usuario WHERE identificacion = '100'"))
                    .contains("uq_usuario_identificacion");
            assertThat(plan("SELECT id FROM implemento_deportivo WHERE codigo_inventario = 'BAL-001'"))
                    .contains("uq_implemento_codigo_inventario");
            assertThat(plan("SELECT id FROM sancion WHERE usuario_id = '" + USUARIO + "' AND estado = 'ACTIVA' "
                    + "AND fecha_fin > now()"))
                    .contains("ix_sancion_usuario_vigente");
            assertThat(plan("SELECT id FROM devolucion ORDER BY fecha_devolucion DESC LIMIT 20"))
                    .contains("ix_devolucion_fecha_devolucion");
            assertThat(plan("SELECT id FROM devolucion WHERE alcance = 'PARCIAL'"))
                    .contains("ix_devolucion_alcance");
            assertThat(plan("SELECT id FROM detalle_devolucion WHERE condicion = 'CON_NOVEDAD'"))
                    .contains("ix_detalle_devolucion_condicion");
            assertThat(plan("SELECT id FROM detalle_prestamo WHERE implemento_id = '" + IMPLEMENTO + "'"))
                    .contains("ix_detalle_prestamo_implemento");
        }

        private String plan(String consulta) {
            return String.join("\n", jdbc.queryForList("EXPLAIN " + consulta, String.class));
        }
    }
}
