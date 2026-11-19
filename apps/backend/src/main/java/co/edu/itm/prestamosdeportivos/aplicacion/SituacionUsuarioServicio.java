package co.edu.itm.prestamosdeportivos.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad.CadenaElegibilidad;
import co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad.ContextoElegibilidad;
import co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad.MotivoElegibilidad;
import co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad.ResultadoElegibilidad;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoSancion;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.persistencia.DetallePrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.PrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.SancionRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import co.edu.itm.prestamosdeportivos.web.dto.SituacionUsuarioDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SituacionUsuarioServicio {
    private final UsuarioRepositorio usuarios; private final PrestamoRepositorio prestamos;
    private final DetallePrestamoRepositorio detalles; private final SancionRepositorio sanciones;
    private final CadenaElegibilidad cadena; private final Clock reloj; private final ConversorFechas conversorFechas;
    public SituacionUsuarioServicio(UsuarioRepositorio u, PrestamoRepositorio p, DetallePrestamoRepositorio d,
            SancionRepositorio s, CadenaElegibilidad c, Clock r, ConversorFechas f){usuarios=u;prestamos=p;detalles=d;sanciones=s;cadena=c;reloj=r;conversorFechas=f;}

    @Transactional(readOnly=true)
    public SituacionUsuarioDto.Respuesta consultar(UUID id){
        var usuario=usuarios.findByIdAndRol(id,Rol.USER).orElseThrow(()->new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO));
        var ahora=reloj.instant(); var sancion=sanciones.buscarVigente(id,ahora);
        long activas=detalles.contarUnidadesPendientesDeUsuario(id);
        long vencidos=prestamos.contarPrestamosVencidosPendientes(id,EstadoPrestamo.FINALIZADO,ahora);
        long prestamosActivos=prestamos.countByUsuarioIdAndEstadoNot(id,EstadoPrestamo.FINALIZADO);
        var contexto=new ContextoElegibilidad(usuario,ahora,activas,prestamosActivos,vencidos,sancion.isPresent(),null,null,null);
        var resultado=cadena.ejecutar(contexto);
        return respuesta(usuario,resultado,activas,prestamosActivos,vencidos,sancion.map(x->new SituacionUsuarioDto.Sancion(conversorFechas.aRespuesta(x.getFechaInicio()),conversorFechas.aRespuesta(x.getFechaFin()))).orElse(null));
    }

    /** Punto común para que BE-10 vuelva a ejecutar toda la cadena al registrar el préstamo. */
    @Transactional(readOnly=true)
    public void validarSolicitudParaPrestamo(UUID usuarioId,List<UUID> implementoIds){
        var usuario=usuarios.findByIdAndRol(usuarioId,Rol.USER).orElse(null); var ahora=reloj.instant();
        var sancion=usuario==null?java.util.Optional.<co.edu.itm.prestamosdeportivos.dominio.Sancion>empty():sanciones.buscarVigente(usuarioId,ahora);
        long activas=usuario==null?0:detalles.contarUnidadesPendientesDeUsuario(usuarioId);
        long vencidos=usuario==null?0:prestamos.contarPrestamosVencidosPendientes(usuarioId,EstadoPrestamo.FINALIZADO,ahora);
        var resultado=cadena.ejecutar(new ContextoElegibilidad(usuario,ahora,activas,0,vencidos,sancion.isPresent(),implementoIds,ahora,ahora.plus(Duration.ofHours(24))));
        if(!resultado.habilitado()){
            var primer=resultado.motivos().get(0);
            throw new ExcepcionDominio(primer.codigo(),primer.mensaje(),resultado.motivos().stream().map(m->new ErrorCampo(m.campo(),m.mensaje())).toList());
        }
    }
    private static SituacionUsuarioDto.Respuesta respuesta(co.edu.itm.prestamosdeportivos.dominio.Usuario u,ResultadoElegibilidad r,long activas,long prestamosActivos,long vencidos,SituacionUsuarioDto.Sancion sancion){
        return new SituacionUsuarioDto.Respuesta(new SituacionUsuarioDto.Usuario(u.getId(),u.getNombres(),u.getApellidos(),u.getIdentificacion()),r.habilitado(),r.motivos().stream().map(m->new SituacionUsuarioDto.Motivo(m.codigo(),m.mensaje())).toList(),activas,4,Math.max(0,4-activas),prestamosActivos,vencidos,sancion);
    }
}
