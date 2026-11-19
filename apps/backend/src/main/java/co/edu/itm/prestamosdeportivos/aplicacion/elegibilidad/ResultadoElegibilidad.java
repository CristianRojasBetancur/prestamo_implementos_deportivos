package co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad;

import java.util.ArrayList;
import java.util.List;

public final class ResultadoElegibilidad {
    private final List<MotivoElegibilidad> motivos = new ArrayList<>();
    public void bloquear(co.edu.itm.prestamosdeportivos.dominio.error.CodigoError codigo, String mensaje, String campo) {
        if (motivos.stream().noneMatch(m -> m.codigo() == codigo && java.util.Objects.equals(m.campo(), campo)))
            motivos.add(new MotivoElegibilidad(codigo, mensaje, campo));
    }
    public List<MotivoElegibilidad> motivos() { return List.copyOf(motivos); }
    public boolean habilitado() { return motivos.isEmpty(); }
}
