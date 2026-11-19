package co.edu.itm.prestamosdeportivos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import org.junit.jupiter.api.Test;

class EstadoImplementoTest {

    @Test
    void disponibleSoloPasaAMantenimientoOBajaPorAdministracion() {
        assertThat(EstadoImplemento.DISPONIBLE.admite(EstadoImplemento.PRESTADO,
                EventoEstadoImplemento.PRESTAMO)).isTrue();
        assertThat(EstadoImplemento.EN_MANTENIMIENTO.admite(EstadoImplemento.PRESTADO,
                EventoEstadoImplemento.PRESTAMO)).isFalse();
        assertThat(EstadoImplemento.DISPONIBLE.admite(EstadoImplemento.EN_MANTENIMIENTO,
                EventoEstadoImplemento.ADMINISTRACION)).isTrue();
        assertThat(EstadoImplemento.DISPONIBLE.admite(EstadoImplemento.DADO_DE_BAJA,
                EventoEstadoImplemento.ADMINISTRACION)).isTrue();
        assertThat(EstadoImplemento.DISPONIBLE.admite(EstadoImplemento.PRESTADO,
                EventoEstadoImplemento.ADMINISTRACION)).isFalse();
    }

    @Test
    void prestadoSoloSeLiberaPorDevolucionONovedad() {
        assertThat(EstadoImplemento.PRESTADO.admite(EstadoImplemento.DISPONIBLE,
                EventoEstadoImplemento.ADMINISTRACION)).isFalse();
        assertThat(EstadoImplemento.PRESTADO.admite(EstadoImplemento.DISPONIBLE,
                EventoEstadoImplemento.DEVOLUCION)).isTrue();
        assertThat(EstadoImplemento.PRESTADO.admite(EstadoImplemento.EN_MANTENIMIENTO,
                EventoEstadoImplemento.INCIDENCIA)).isTrue();
        assertThat(EstadoImplemento.PRESTADO.admite(EstadoImplemento.DADO_DE_BAJA,
                EventoEstadoImplemento.INCIDENCIA)).isTrue();
    }

    @Test
    void mantenimientoPuedeHabilitarseYBajaEsTerminal() {
        assertThat(EstadoImplemento.EN_MANTENIMIENTO.admite(EstadoImplemento.DISPONIBLE,
                EventoEstadoImplemento.ADMINISTRACION)).isTrue();
        for (EstadoImplemento destino : EstadoImplemento.values()) {
            assertThat(EstadoImplemento.DADO_DE_BAJA.admite(destino, EventoEstadoImplemento.ADMINISTRACION)).isFalse();
        }
    }

    @Test
    void soloDisponibleEsPrestableYUnPrestadoNoSePuedeLiberarAdministrativamente() {
        Categoria categoria = new Categoria("Balones", null);
        ImplementoDeportivo unidad = new ImplementoDeportivo("BAL-01", "Balón", null, categoria,
                EstadoImplemento.PRESTADO);

        assertThat(unidad.esPrestable()).isFalse();
        assertThatThrownBy(() -> unidad.cambiarEstado(EstadoImplemento.DISPONIBLE,
                EventoEstadoImplemento.ADMINISTRACION)).isInstanceOf(ExcepcionDominio.class);
    }
}
