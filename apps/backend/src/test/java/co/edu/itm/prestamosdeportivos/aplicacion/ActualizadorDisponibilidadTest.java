package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.List;

import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EventoEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.HistorialEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.HistorialEstadoImplementoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActualizadorDisponibilidadTest {

    @Mock private ImplementoDeportivoRepositorio implementos;
    @Mock private HistorialEstadoImplementoRepositorio historial;

    private ActualizadorDisponibilidad actualizador;
    private Usuario operador;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        actualizador = new ActualizadorDisponibilidad(implementos, historial, new PoliticaCierrePrestamo());
        operador = Usuario.conAcceso("P-01", "Prestador", "Prueba", "p@itm.edu.co", "hash", Rol.PRESTADOR);
        categoria = new Categoria("Balones", null);
    }

    @Test
    void reservaCambiaEstadoYAuditaElOperador() {
        ImplementoDeportivo unidad = unidad(EstadoImplemento.DISPONIBLE);

        actualizador.reservar(List.of(unidad), operador);

        assertThat(unidad.getEstado()).isEqualTo(EstadoImplemento.PRESTADO);
        ArgumentCaptor<Iterable<HistorialEstadoImplemento>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(historial).saveAll(captor.capture());
        HistorialEstadoImplemento evento = captor.getValue().iterator().next();
        assertThat(evento.getEvento()).isEqualTo(EventoEstadoImplemento.PRESTAMO);
        assertThat(evento.getOperador()).isSameAs(operador);
    }

    @Test
    void devolucionBuenaLiberaSoloLaUnidad() {
        ImplementoDeportivo unidad = unidad(EstadoImplemento.PRESTADO);

        actualizador.liberar(unidad, CondicionDevolucion.BUENO, operador);

        assertThat(unidad.getEstado()).isEqualTo(EstadoImplemento.DISPONIBLE);
    }

    @Test
    void devolucionConNovedadMantieneLaUnidadNoDisponible() {
        ImplementoDeportivo unidad = unidad(EstadoImplemento.PRESTADO);

        actualizador.liberar(unidad, CondicionDevolucion.CON_NOVEDAD, operador);

        assertThat(unidad.getEstado()).isEqualTo(EstadoImplemento.EN_MANTENIMIENTO);
    }

    @Test
    void incidenciaPuedeDarDeBajaLaUnidadPeroNoLiberarla() {
        ImplementoDeportivo unidad = unidad(EstadoImplemento.PRESTADO);

        actualizador.marcarNoDisponible(unidad, EstadoImplemento.DADO_DE_BAJA, operador, "Pérdida reportada.");

        assertThat(unidad.getEstado()).isEqualTo(EstadoImplemento.DADO_DE_BAJA);
        assertThat(unidad.esPrestable()).isFalse();
    }

    @Test
    void incidenciaNoAceptaUnDestinoDisponible() {
        ImplementoDeportivo unidad = unidad(EstadoImplemento.PRESTADO);

        assertThatThrownBy(() -> actualizador.marcarNoDisponible(unidad, EstadoImplemento.DISPONIBLE, operador, "x"))
                .isInstanceOf(ExcepcionDominio.class)
                .satisfies(error -> assertThat(((ExcepcionDominio) error).getCodigo())
                        .isEqualTo(CodigoError.TRANSICION_INVALIDA));
    }

    private ImplementoDeportivo unidad(EstadoImplemento estado) {
        return new ImplementoDeportivo("BAL-01", "Balón", null, categoria, estado);
    }
}
