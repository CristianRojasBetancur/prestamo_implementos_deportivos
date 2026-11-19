package co.edu.itm.prestamosdeportivos.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarPrestadorSolicitud(
        @NotBlank @Size(max = 120) String nombres,
        @NotBlank @Size(max = 120) String apellidos,
        @NotBlank @Size(max = 40) String identificacion,
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Size(min = 8) String contrasenaInicial) {
}
