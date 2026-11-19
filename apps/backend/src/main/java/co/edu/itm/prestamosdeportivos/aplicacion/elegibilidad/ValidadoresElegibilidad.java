package co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import co.edu.itm.prestamosdeportivos.dominio.EstadoDetallePrestamo;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EstadoIncidencia;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.persistencia.DetallePrestamoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.IncidenciaRepositorio;
import org.springframework.stereotype.Component;

/** Validadores concretos de la cadena. */
public final class ValidadoresElegibilidad {
    private ValidadoresElegibilidad() {}
    @Component public static class Solicitud implements ValidadorElegibilidad {
        public int orden(){return 10;}
        public void validar(ContextoElegibilidad c, ResultadoElegibilidad r){
            List<UUID> ids=c.implementoIds(); if(ids==null)return;
            if(ids.isEmpty()||ids.size()>4)r.bloquear(CodigoError.VALIDACION_FALLIDA,"La solicitud debe incluir entre 1 y 4 unidades.","implementoIds");
            if(new HashSet<>(ids).size()!=ids.size())r.bloquear(CodigoError.VALIDACION_FALLIDA,"La solicitud contiene unidades duplicadas.","implementoIds");
        }
    }
    @Component public static class UsuarioHabilitado implements ValidadorElegibilidad {
        public int orden(){return 20;}
        public void validar(ContextoElegibilidad c, ResultadoElegibilidad r){
            if(c.usuario()==null||c.usuario().getRol()!=Rol.USER||!c.usuario().isActivo())r.bloquear(CodigoError.USUARIO_NO_HABILITADO,"El usuario no está habilitado para recibir préstamos.","usuarioId");
        }
    }
    @Component public static class Suspension implements ValidadorElegibilidad {
        public int orden(){return 30;}
        public void validar(ContextoElegibilidad c, ResultadoElegibilidad r){
            if(c.usuario()!=null&&(c.sancionVigente()||(c.usuario().getSuspendidoHasta()!=null&&c.usuario().getSuspendidoHasta().isAfter(c.ahora()))))r.bloquear(CodigoError.USUARIO_SANCIONADO,"El usuario tiene una suspensión vigente.","usuarioId");
        }
    }
    @Component public static class Vencidos implements ValidadorElegibilidad {
        public int orden(){return 40;}
        public void validar(ContextoElegibilidad c, ResultadoElegibilidad r){if(c.prestamosVencidos()>0)r.bloquear(CodigoError.USUARIO_CON_PRESTAMOS_VENCIDOS,"El usuario tiene préstamos vencidos pendientes de devolución.","usuarioId");}
    }
    @Component public static class Limite implements ValidadorElegibilidad {
        public int orden(){return 50;}
        public void validar(ContextoElegibilidad c, ResultadoElegibilidad r){int solicitadas=c.implementoIds()==null?0:c.implementoIds().size();if(c.unidadesActivas()+solicitadas>4)r.bloquear(CodigoError.LIMITE_IMPLEMENTOS_EXCEDIDO,"El usuario superaría el máximo de 4 implementos activos.","implementoIds");}
    }
    @Component public static class Fechas implements ValidadorElegibilidad {
        public int orden(){return 60;}
        public void validar(ContextoElegibilidad c, ResultadoElegibilidad r){if(c.fechaPrestamo()==null&&c.fechaLimite()==null)return;if(c.fechaPrestamo()==null||c.fechaLimite()==null||c.fechaPrestamo().isAfter(c.ahora())||!c.fechaLimite().equals(c.fechaPrestamo().plus(Duration.ofHours(24))))r.bloquear(CodigoError.VALIDACION_FALLIDA,"El préstamo debe durar como máximo 24 horas desde su inicio.","fechaLimiteDevolucion");}
    }
    @Component public static class Disponibilidad implements ValidadorElegibilidad {
        private final ImplementoDeportivoRepositorio implementos; private final DetallePrestamoRepositorio detalles; private final IncidenciaRepositorio incidencias;
        public Disponibilidad(ImplementoDeportivoRepositorio i,DetallePrestamoRepositorio d,IncidenciaRepositorio x){implementos=i;detalles=d;incidencias=x;}
        public int orden(){return 70;}
        public void validar(ContextoElegibilidad c, ResultadoElegibilidad r){if(c.implementoIds()==null)return;for(UUID id:c.implementoIds()){var item=implementos.findById(id);if(item.isEmpty()){r.bloquear(CodigoError.RECURSO_NO_ENCONTRADO,"No existe la unidad solicitada: "+id,"implementoIds");continue;}if(item.get().getEstado()!=EstadoImplemento.DISPONIBLE||detalles.existsByImplementoIdAndEstado(id,EstadoDetallePrestamo.PENDIENTE_DEVOLUCION)||incidencias.existsByImplementoIdAndEstado(id,EstadoIncidencia.REGISTRADA))r.bloquear(CodigoError.IMPLEMENTO_NO_DISPONIBLE,"La unidad "+item.get().getCodigoInventario()+" no está disponible.","implementoIds");}}
    }
}
