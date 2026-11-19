package co.edu.itm.prestamosdeportivos.aplicacion.sanciones;

import java.time.Duration;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Strategy RN-03: duplica el retraso y respeta el mínimo de dos días. */
@Component
@Order(1)
public class SancionPorRetrasoSinJustificacion implements PoliticaSancion {

    private static final Duration DURACION_MINIMA = Duration.ofDays(2);

    @Override
    public boolean aplica(ContextoSancion contexto) {
        return !contexto.devolucion().isRetrasoJustificado()
                && contexto.ahora().isAfter(contexto.prestamo().getFechaLimiteDevolucion());
    }

    @Override
    public Optional<ResultadoSancion> calcular(ContextoSancion contexto) {
        long minutosRetraso = Math.max(1,
                Duration.between(contexto.prestamo().getFechaLimiteDevolucion(), contexto.ahora()).toMinutes());
        Duration duracion = DURACION_MINIMA.compareTo(Duration.ofMinutes(minutosRetraso * 2)) >= 0
                ? DURACION_MINIMA
                : Duration.ofMinutes(minutosRetraso * 2);
        return Optional.of(new ResultadoSancion(minutosRetraso, duracion));
    }
}
