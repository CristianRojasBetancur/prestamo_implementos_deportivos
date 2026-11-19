package co.edu.itm.prestamosdeportivos.aplicacion.elegibilidad;

import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;

public record MotivoElegibilidad(CodigoError codigo, String mensaje, String campo) {
}
