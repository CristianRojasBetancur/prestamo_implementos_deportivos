package co.edu.itm.prestamosdeportivos.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import co.edu.itm.prestamosdeportivos.configuracion.FiltroCorrelacion;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.dominio.error.TipoError;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/** Pruebas unitarias del mapeo de excepciones a estados y del formato de error. */
class MapeoErroresTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-31T23:00:00Z"), ZoneId.of("America/Bogota"));

    private final ManejadorGlobalErrores manejador = new ManejadorGlobalErrores(new FabricaProblemas(RELOJ));

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @ParameterizedTest
    @EnumSource(CodigoError.class)
    @DisplayName("Todo código del catálogo tiene título, detalle y un estado HTTP de error")
    void catalogoCompleto(CodigoError codigo) {
        assertThat(codigo.titulo()).isNotBlank();
        assertThat(codigo.detallePorDefecto()).isNotBlank();
        assertThat(EstadoHttpPorTipoError.de(codigo.tipo()).isError()).isTrue();
        assertThat(codigo.slug()).matches("[a-z-]+");
    }

    @Test
    @DisplayName("Cada tipo de error se traduce al estado HTTP de BE-01 §4.9")
    void tiposAEstados() {
        assertThat(EstadoHttpPorTipoError.de(TipoError.SOLICITUD_INVALIDA)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(EstadoHttpPorTipoError.de(TipoError.NO_AUTENTICADO)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(EstadoHttpPorTipoError.de(TipoError.ACCESO_DENEGADO)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(EstadoHttpPorTipoError.de(TipoError.NO_ENCONTRADO)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(EstadoHttpPorTipoError.de(TipoError.CONFLICTO)).isEqualTo(HttpStatus.CONFLICT);
        assertThat(EstadoHttpPorTipoError.de(TipoError.REGLA_NEGOCIO).value()).isEqualTo(422);
        assertThat(EstadoHttpPorTipoError.de(TipoError.INTERNO)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(EstadoHttpPorTipoError.de(TipoError.NO_DISPONIBLE)).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Los estados de las excepciones estándar de Spring MVC se traducen a códigos del catálogo")
    void estadosACodigos() {
        assertThat(ManejadorGlobalErrores.codigoParaEstado(HttpStatus.BAD_REQUEST)).isEqualTo(CodigoError.SOLICITUD_INVALIDA);
        assertThat(ManejadorGlobalErrores.codigoParaEstado(HttpStatus.NOT_FOUND)).isEqualTo(CodigoError.RECURSO_NO_ENCONTRADO);
        assertThat(ManejadorGlobalErrores.codigoParaEstado(HttpStatus.METHOD_NOT_ALLOWED)).isEqualTo(CodigoError.METODO_NO_PERMITIDO);
        assertThat(ManejadorGlobalErrores.codigoParaEstado(HttpStatus.UNSUPPORTED_MEDIA_TYPE))
                .isEqualTo(CodigoError.TIPO_CONTENIDO_NO_SOPORTADO);
        assertThat(ManejadorGlobalErrores.codigoParaEstado(HttpStatus.BAD_GATEWAY)).isEqualTo(CodigoError.ERROR_INTERNO);
    }

    @Test
    @DisplayName("Una violación de unicidad (SQLState 23505) se distingue de otras violaciones de integridad")
    void deteccionUnicidad() {
        var unicidad = new DataIntegrityViolationException("x", new SQLException("duplicado", "23505"));
        var claveForanea = new DataIntegrityViolationException("x", new SQLException("fk", "23503"));

        assertThat(ManejadorGlobalErrores.esViolacionDeUnicidad(unicidad)).isTrue();
        assertThat(ManejadorGlobalErrores.esViolacionDeUnicidad(claveForanea)).isFalse();
    }

    @Test
    @DisplayName("Una excepción de dominio conserva su código, mensaje y errores por campo")
    void excepcionDominio() {
        var excepcion = new ExcepcionDominio(CodigoError.INCIDENCIA_REQUERIDA, "El implemento PET-014 requiere incidencia.",
                List.of(new ErrorCampo("items[1].incidencia", "es obligatorio")));

        ProblemaRespuesta problema = cuerpo(manejador.manejarDominio(excepcion, solicitud("/api/v1/prestamos/1/devoluciones")));

        assertThat(problema.status()).isEqualTo(422);
        assertThat(problema.codigo()).isEqualTo("INCIDENCIA_REQUERIDA");
        assertThat(problema.type()).isEqualTo("https://prestamos.itm.edu.co/errores/incidencia-requerida");
        assertThat(problema.detail()).isEqualTo("El implemento PET-014 requiere incidencia.");
        assertThat(problema.instance()).isEqualTo("/api/v1/prestamos/1/devoluciones");
        assertThat(problema.errores()).containsExactly(new ErrorCampo("items[1].incidencia", "es obligatorio"));
    }

    @Test
    @DisplayName("Un error inesperado responde 500 genérico, sin filtrar el mensaje técnico, con timestamp y correlación")
    void errorInesperadoNoFiltraDetalles() {
        MDC.put(FiltroCorrelacion.CLAVE_MDC, "corr-123");

        ResponseEntity<Object> respuesta = manejador.manejarInesperado(
                new IllegalStateException("SELECT contrasena_hash FROM usuario WHERE token='abc'"), solicitud("/api/v1/x"));
        ProblemaRespuesta problema = cuerpo(respuesta);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(problema.codigo()).isEqualTo("ERROR_INTERNO");
        assertThat(problema.detail()).doesNotContain("SELECT", "contrasena", "token");
        assertThat(problema.timestamp()).isEqualTo("2026-08-31T18:00:00-05:00");
        assertThat(problema.correlacionId()).isEqualTo("corr-123");
        assertThat(problema.errores()).isNull();
    }

    @Test
    @DisplayName("Paginación: valores por defecto y rechazo de campos de orden no permitidos")
    void paginacion() {
        Pageable porDefecto = new ParametrosPagina(null, null, null)
                .aPageable(Set.of("fechaPrestamo"), Sort.by(Sort.Direction.DESC, "fechaPrestamo"));
        assertThat(porDefecto.getPageNumber()).isZero();
        assertThat(porDefecto.getPageSize()).isEqualTo(20);
        assertThat(porDefecto.getSort().getOrderFor("fechaPrestamo")).isNotNull();

        assertThatThrownBy(() -> new ParametrosPagina(0, 10, "contrasenaHash,asc")
                .aPageable(Set.of("fechaPrestamo"), Sort.unsorted()))
                .isInstanceOf(ExcepcionDominio.class)
                .satisfies(ex -> assertThat(((ExcepcionDominio) ex).getErrores()).extracting(ErrorCampo::campo)
                        .containsExactly("orden"));
    }

    private static MockHttpServletRequest solicitud(String ruta) {
        return new MockHttpServletRequest("GET", ruta);
    }

    private static ProblemaRespuesta cuerpo(ResponseEntity<Object> respuesta) {
        return (ProblemaRespuesta) respuesta.getBody();
    }
}
