package co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import co.edu.itm.prestamosdeportivos.dominio.*;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.persistencia.*;
import org.junit.jupiter.api.Test;

class ValidadoresElegibilidadTest {
    private final Instant ahora=Instant.parse("2026-11-19T12:00:00Z");
    private ContextoElegibilidad contexto(Usuario u,List<UUID> ids,long activas,long vencidos,boolean sancion,Instant inicio,Instant limite){return new ContextoElegibilidad(u,ahora,activas,0,vencidos,sancion,ids,inicio,limite);}
    private Usuario usuario(){return Usuario.solicitante("123","Ana","Rojas");}

    @Test void solicitudRequiereUnidadesUnicas(){var r=new ResultadoElegibilidad();new ValidadoresElegibilidad.Solicitud().validar(contexto(usuario(),List.of(),0,0,false,null,null),r);assertThat(r.motivos()).extracting(MotivoElegibilidad::codigo).contains(CodigoError.VALIDACION_FALLIDA);}
    @Test void usuarioInactivoQuedaBloqueado(){var u=usuario();u.desactivar();var r=new ResultadoElegibilidad();new ValidadoresElegibilidad.UsuarioHabilitado().validar(contexto(u,null,0,0,false,null,null),r);assertThat(r.motivos()).extracting(MotivoElegibilidad::codigo).contains(CodigoError.USUARIO_NO_HABILITADO);}
    @Test void sancionVigenteQuedaBloqueada(){var r=new ResultadoElegibilidad();new ValidadoresElegibilidad.Suspension().validar(contexto(usuario(),null,0,0,true,null,null),r);assertThat(r.motivos()).extracting(MotivoElegibilidad::codigo).contains(CodigoError.USUARIO_SANCIONADO);}
    @Test void vencidoQuedaBloqueado(){var r=new ResultadoElegibilidad();new ValidadoresElegibilidad.Vencidos().validar(contexto(usuario(),null,0,1,false,null,null),r);assertThat(r.motivos()).extracting(MotivoElegibilidad::codigo).contains(CodigoError.USUARIO_CON_PRESTAMOS_VENCIDOS);}
    @Test void limiteCuatroUnidades(){var r=new ResultadoElegibilidad();new ValidadoresElegibilidad.Limite().validar(contexto(usuario(),List.of(UUID.randomUUID()),4,0,false,null,null),r);assertThat(r.motivos()).extracting(MotivoElegibilidad::codigo).contains(CodigoError.LIMITE_IMPLEMENTOS_EXCEDIDO);}
    @Test void fechasSeLimitanA24Horas(){var r=new ResultadoElegibilidad();new ValidadoresElegibilidad.Fechas().validar(contexto(usuario(),null,0,0,false,ahora,ahora.plusSeconds(25*3600)),r);assertThat(r.motivos()).extracting(MotivoElegibilidad::codigo).contains(CodigoError.VALIDACION_FALLIDA);}
    @Test void disponibilidadIdentificaUnidadNoPrestable(){var item=mock(ImplementoDeportivo.class);when(item.getEstado()).thenReturn(EstadoImplemento.EN_MANTENIMIENTO);when(item.getCodigoInventario()).thenReturn("BAL-1");var ir=mock(ImplementoDeportivoRepositorio.class);var dr=mock(DetallePrestamoRepositorio.class);var xr=mock(IncidenciaRepositorio.class);var id=UUID.randomUUID();when(ir.findById(id)).thenReturn(Optional.of(item));var r=new ResultadoElegibilidad();new ValidadoresElegibilidad.Disponibilidad(ir,dr,xr).validar(contexto(usuario(),List.of(id),0,0,false,null,null),r);assertThat(r.motivos()).extracting(MotivoElegibilidad::codigo).contains(CodigoError.IMPLEMENTO_NO_DISPONIBLE);assertThat(r.motivos().get(0).mensaje()).contains("BAL-1");}
    @Test void cadenaEjecutaValidadorasEnOrdenYAcumulaMotivos(){var visita=new java.util.ArrayList<Integer>();ValidadorElegibilidad tarde=new ValidadorElegibilidad(){public int orden(){return 2;}public void validar(ContextoElegibilidad c,ResultadoElegibilidad r){visita.add(2);}};ValidadorElegibilidad pronto=new ValidadorElegibilidad(){public int orden(){return 1;}public void validar(ContextoElegibilidad c,ResultadoElegibilidad r){visita.add(1);r.bloquear(CodigoError.USUARIO_SANCIONADO,"bloqueo",null);}};var cadena=new CadenaElegibilidad(List.of(tarde,pronto));var r=cadena.ejecutar(contexto(usuario(),null,0,0,false,null,null));assertThat(visita).containsExactly(1,2);assertThat(r.habilitado()).isFalse();}
}
