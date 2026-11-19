package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import co.edu.itm.prestamosdeportivos.persistencia.DisponibilidadProyeccion;
import co.edu.itm.prestamosdeportivos.persistencia.DisponibilidadRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServicioTest {

    @Mock private DisponibilidadRepositorio repositorio;
    @InjectMocks private DisponibilidadServicio servicio;

    @Test
    void normalizaBusquedaYCompletaOrdenDeterminista() {
        UUID categoriaId = UUID.randomUUID();
        when(repositorio.buscarDisponibles(eq("futbol"), eq(categoriaId), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<DisponibilidadProyeccion> resultado = servicio.consultar("  FÚTBOL  ", categoriaId,
                PageRequest.of(2, 10, Sort.by("nombre").ascending()));

        assertThat(resultado.getContent()).isEmpty();
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscarDisponibles(eq("futbol"), eq(categoriaId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty))
                .containsExactly("nombre", "categoriaNombre", "categoriaId");
    }

    @Test
    void dejaBusquedaVaciaComoFiltroAusente() {
        when(repositorio.buscarDisponibles(eq(null), eq(null), any(Pageable.class))).thenReturn(Page.empty());

        servicio.consultar("   ", null, PageRequest.of(0, 20));

        verify(repositorio).buscarDisponibles(eq(null), eq(null), any(Pageable.class));
    }
}
