package co.edu.itm.prestamosdeportivos.aplicacion.sanciones;

import java.util.Optional;

/** Strategy para seleccionar y calcular la sanción aplicable a una devolución. */
public interface PoliticaSancion {

    boolean aplica(ContextoSancion contexto);

    Optional<ResultadoSancion> calcular(ContextoSancion contexto);
}
