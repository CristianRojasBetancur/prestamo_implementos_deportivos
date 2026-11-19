package co.edu.itm.prestamosdeportivos.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ActualizarPrestadorSolicitud(
        @Size(max = 120) String nombres,
        @Size(max = 120) String apellidos,
        @Size(max = 40) String identificacion,
        @Email @Size(max = 254) String correo) {
    public boolean vacia() {
        return nombres == null && apellidos == null && identificacion == null && correo == null;
    }
}
