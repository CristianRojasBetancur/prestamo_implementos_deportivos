package co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad;

/** Eslabón de Chain of Responsibility; el orden lo establece {@link CadenaElegibilidad}. */
public interface ValidadorElegibilidad {
    int orden();
    void validar(ContextoElegibilidad contexto, ResultadoElegibilidad resultado);
}
