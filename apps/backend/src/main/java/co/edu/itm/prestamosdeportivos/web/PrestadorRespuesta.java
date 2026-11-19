package co.edu.itm.prestamosdeportivos.web;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PrestadorRespuesta(
        UUID id,
        String nombres,
        String apellidos,
        String identificacion,
        String correo,
        String rol,
        String estado,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {
}
