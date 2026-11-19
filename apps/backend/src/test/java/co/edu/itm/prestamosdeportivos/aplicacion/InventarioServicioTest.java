package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.HistorialEstadoImplementoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventarioServicioTest {

    @Mock private ImplementoDeportivoRepositorio implementos;
    @Mock private CategoriaRepositorio categorias;
    @Mock private HistorialEstadoImplementoRepositorio historial;
    @InjectMocks private InventarioServicio servicio;

    @Test
    void creaUnidadDisponibleConCategoriaActiva() {
        UUID categoriaId = UUID.randomUUID();
        Categoria categoria = new Categoria("Balones", null);
        when(implementos.findByCodigoInventario("BAL-01")).thenReturn(Optional.empty());
        when(categorias.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(implementos.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var creada = servicio.crear("BAL-01", " Balón ", null, categoriaId, null);

        assertThat(creada.getEstado()).isEqualTo(EstadoImplemento.DISPONIBLE);
        assertThat(creada.getNombre()).isEqualTo("Balón");
        assertThat(creada.esPrestable()).isTrue();
    }

    @Test
    void rechazaCodigoDuplicadoConCampo() {
        var existente = org.mockito.Mockito.mock(co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo.class);
        when(existente.getId()).thenReturn(UUID.randomUUID());
        when(implementos.findByCodigoInventario("BAL-01")).thenReturn(Optional.of(existente));
        assertThatThrownBy(() -> servicio.crear("BAL-01", "Balón", null, UUID.randomUUID(), null))
                .isInstanceOf(ExcepcionDominio.class)
                .satisfies(ex -> {
                    var error = (ExcepcionDominio) ex;
                    assertThat(error.getCodigo()).isEqualTo(CodigoError.CONFLICTO_UNICIDAD);
                    assertThat(error.getErrores()).extracting("campo").contains("codigoInventario");
                });
    }

    @Test
    void rechazaEstadoInicialPrestado() {
        when(implementos.findByCodigoInventario("BAL-01")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.crear("BAL-01", "Balón", null, UUID.randomUUID(), EstadoImplemento.PRESTADO))
                .isInstanceOf(ExcepcionDominio.class)
                .satisfies(ex -> assertThat(((ExcepcionDominio) ex).getCodigo()).isEqualTo(CodigoError.VALIDACION_FALLIDA));
    }

    @Test
    void registraHistorialAlCambiarEstadoAdministrativamente() {
        UUID id = UUID.randomUUID();
        var unidad = new co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo("BAL-01", "Balón", null,
                new Categoria("Balones", null), EstadoImplemento.DISPONIBLE);
        when(implementos.findById(id)).thenReturn(Optional.of(unidad));
        when(historial.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        var actualizada = servicio.cambiarEstado(id, EstadoImplemento.EN_MANTENIMIENTO, "  Desgaste  ");

        assertThat(actualizada.getEstado()).isEqualTo(EstadoImplemento.EN_MANTENIMIENTO);
        ArgumentCaptor<co.edu.itm.prestamosdeportivos.dominio.HistorialEstadoImplemento> captor =
                ArgumentCaptor.forClass(co.edu.itm.prestamosdeportivos.dominio.HistorialEstadoImplemento.class);
        verify(historial).save(captor.capture());
        assertThat(captor.getValue().getEstadoAnterior()).isEqualTo(EstadoImplemento.DISPONIBLE);
        assertThat(captor.getValue().getMotivo()).isEqualTo("Desgaste");
    }
}
