package co.edu.itm.prestamosdeportivos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad.*;
import co.edu.itm.prestamosdeportivos.dominio.*;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.persistencia.*;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import org.junit.jupiter.api.Test;

class SituacionUsuarioServicioTest {
    private final Instant ahora=Instant.parse("2026-11-19T12:00:00Z");
    private final UsuarioRepositorio usuarios=mock(UsuarioRepositorio.class);
    private final PrestamoRepositorio prestamos=mock(PrestamoRepositorio.class);
    private final DetallePrestamoRepositorio detalles=mock(DetallePrestamoRepositorio.class);
    private final SancionRepositorio sanciones=mock(SancionRepositorio.class);
    private final CadenaElegibilidad cadena=new CadenaElegibilidad(List.of(new ValidadoresElegibilidad.UsuarioHabilitado(),new ValidadoresElegibilidad.Vencidos()));
    private final SituacionUsuarioServicio servicio=new SituacionUsuarioServicio(usuarios,prestamos,detalles,sanciones,cadena,Clock.fixed(ahora,ZoneOffset.UTC),new ConversorFechas(Clock.fixed(ahora,ZoneOffset.UTC)));

    @Test void consultaExponeLimiteYMotivoSinMutarUsuario(){UUID id=UUID.randomUUID();var usuario=Usuario.solicitante("123","Ana","Rojas");when(usuarios.findByIdAndRol(id,Rol.USER)).thenReturn(Optional.of(usuario));when(sanciones.buscarVigente(id,ahora)).thenReturn(Optional.empty());when(detalles.contarUnidadesPendientesDeUsuario(id)).thenReturn(2L);when(prestamos.contarPrestamosVencidosPendientes(id,EstadoPrestamo.FINALIZADO,ahora)).thenReturn(1L);when(prestamos.countByUsuarioIdAndEstadoNot(id,EstadoPrestamo.FINALIZADO)).thenReturn(1L);
        var respuesta=servicio.consultar(id);
        assertThat(respuesta.habilitado()).isFalse();assertThat(respuesta.unidadesDisponiblesParaPrestar()).isEqualTo(2);assertThat(respuesta.motivos()).extracting("codigo").contains(CodigoError.USUARIO_CON_PRESTAMOS_VENCIDOS);verify(usuarios,never()).save(any());}
    @Test void validacionDePrestamoUsaLaMismaCadena(){UUID user=UUID.randomUUID(),unit=UUID.randomUUID();when(usuarios.findByIdAndRol(user,Rol.USER)).thenReturn(Optional.of(Usuario.solicitante("123","Ana","Rojas")));when(sanciones.buscarVigente(eq(user),any())).thenReturn(Optional.empty());when(detalles.contarUnidadesPendientesDeUsuario(user)).thenReturn(0L);when(prestamos.contarPrestamosVencidosPendientes(eq(user),eq(EstadoPrestamo.FINALIZADO),any())).thenReturn(0L);var cadenaEspia=spy(cadena);var servicioPrueba=new SituacionUsuarioServicio(usuarios,prestamos,detalles,sanciones,cadenaEspia,Clock.fixed(ahora,ZoneOffset.UTC),new ConversorFechas(Clock.fixed(ahora,ZoneOffset.UTC)));
        servicioPrueba.validarSolicitudParaPrestamo(user,List.of(unit));
        verify(cadenaEspia).ejecutar(any(ContextoElegibilidad.class));}
}
