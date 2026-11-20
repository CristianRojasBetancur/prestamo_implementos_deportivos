package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import co.edu.itm.prestamosdeportivos.dominio.CondicionDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EventoEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.HistorialEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.HistorialEstadoImplementoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mediator de disponibilidad. Coordina cambios de unidades dentro de la
 * transacción del caso de uso, pero delega las transiciones a State y el cierre
 * global del préstamo a PoliticaCierrePrestamo.
 */
@Service
public class ActualizadorDisponibilidad
        implements ReservaDisponibilidad, LiberacionDisponibilidad, IncidenciaDisponibilidad, CierrePrestamo {

    private final ImplementoDeportivoRepositorio implementos;
    private final HistorialEstadoImplementoRepositorio historial;
    private final PoliticaCierrePrestamo politicaCierre;

    public ActualizadorDisponibilidad(ImplementoDeportivoRepositorio implementos,
            HistorialEstadoImplementoRepositorio historial, PoliticaCierrePrestamo politicaCierre) {
        this.implementos = implementos;
        this.historial = historial;
        this.politicaCierre = politicaCierre;
    }

    @Override
    @Transactional
    public void reservar(Collection<ImplementoDeportivo> unidades, Usuario operador) {
        Objects.requireNonNull(unidades, "unidades");
        List<HistorialEstadoImplemento> eventos = new ArrayList<>();
        unidades.stream().sorted(Comparator.comparing(ImplementoDeportivo::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(unidad -> eventos.add(cambiar(unidad, EstadoImplemento.PRESTADO,
                        EventoEstadoImplemento.PRESTAMO, operador, "Entrega asignada a un préstamo.")));
        guardar(unidades, eventos);
    }

    @Override
    @Transactional
    public void liberar(ImplementoDeportivo unidad, CondicionDevolucion condicion, Usuario operador) {
        Objects.requireNonNull(condicion, "condicion");
        EstadoImplemento destino = condicion == CondicionDevolucion.BUENO
                ? EstadoImplemento.DISPONIBLE : EstadoImplemento.EN_MANTENIMIENTO;
        EventoEstadoImplemento evento = condicion == CondicionDevolucion.BUENO
                ? EventoEstadoImplemento.DEVOLUCION : EventoEstadoImplemento.INCIDENCIA;
        String motivo = condicion == CondicionDevolucion.BUENO
                ? "Unidad recibida en buen estado." : "Unidad recibida con novedad.";
        guardar(List.of(unidad), List.of(cambiar(unidad, destino, evento, operador, motivo)));
    }

    @Override
    @Transactional
    public void marcarNoDisponible(ImplementoDeportivo unidad, EstadoImplemento destino, Usuario operador,
            String motivo) {
        if (destino != EstadoImplemento.EN_MANTENIMIENTO && destino != EstadoImplemento.DADO_DE_BAJA) {
            throw new ExcepcionDominio(CodigoError.TRANSICION_INVALIDA,
                    "Una incidencia solo puede dejar la unidad no disponible.");
        }
        guardar(List.of(unidad), List.of(cambiar(unidad, destino, EventoEstadoImplemento.INCIDENCIA, operador,
                motivo)));
    }

    @Override
    public void actualizar(Prestamo prestamo, java.time.Instant ahora) {
        politicaCierre.actualizar(prestamo, ahora);
    }

    private HistorialEstadoImplemento cambiar(ImplementoDeportivo unidad, EstadoImplemento destino,
            EventoEstadoImplemento evento, Usuario operador, String motivo) {
        Objects.requireNonNull(unidad, "unidad");
        Objects.requireNonNull(operador, "operador");
        EstadoImplemento anterior = unidad.getEstado();
        unidad.cambiarEstado(destino, evento);
        return new HistorialEstadoImplemento(unidad, anterior, destino, evento, motivo, operador);
    }

    private void guardar(Collection<ImplementoDeportivo> unidades, Collection<HistorialEstadoImplemento> eventos) {
        implementos.saveAll(unidades);
        historial.saveAll(eventos);
    }
}
