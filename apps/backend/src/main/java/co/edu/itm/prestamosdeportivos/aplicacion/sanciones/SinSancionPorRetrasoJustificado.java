package co.edu.itm.prestamosdeportivos.aplicacion.sanciones;

import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Strategy explícita para el tratamiento de una devolución justificada. */
@Component
@Order(0)
public class SinSancionPorRetrasoJustificado implements PoliticaSancion {

    @Override
    public boolean aplica(ContextoSancion contexto) {
        return contexto.devolucion().isRetrasoJustificado();
    }

    @Override
    public Optional<ResultadoSancion> calcular(ContextoSancion contexto) {
        return Optional.empty();
    }
}
