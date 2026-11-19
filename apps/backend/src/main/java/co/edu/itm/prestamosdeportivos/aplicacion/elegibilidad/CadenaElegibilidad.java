package co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Composición ordenada de validadores independientes. */
@Component
public class CadenaElegibilidad {
    private final List<ValidadorElegibilidad> validadores;
    public CadenaElegibilidad(List<ValidadorElegibilidad> validadores) {
        this.validadores = validadores.stream().sorted(Comparator.comparingInt(ValidadorElegibilidad::orden)).toList();
    }
    public ResultadoElegibilidad ejecutar(ContextoElegibilidad contexto) {
        ResultadoElegibilidad resultado = new ResultadoElegibilidad();
        validadores.forEach(v -> v.validar(contexto, resultado));
        return resultado;
    }
    List<Integer> orden() { return validadores.stream().map(ValidadorElegibilidad::orden).toList(); }
}
