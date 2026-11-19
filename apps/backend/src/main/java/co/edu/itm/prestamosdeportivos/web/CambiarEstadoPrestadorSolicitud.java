package co.edu.itm.prestamosdeportivos.web;

import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestador;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoPrestadorSolicitud(@NotNull EstadoPrestador estado) {
}
