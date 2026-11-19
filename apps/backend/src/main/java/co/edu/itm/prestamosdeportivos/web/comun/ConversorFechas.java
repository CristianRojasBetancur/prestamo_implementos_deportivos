package co.edu.itm.prestamosdeportivos.web.comun;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

/**
 * Convierte instantes persistidos (UTC) a {@link OffsetDateTime} en la zona
 * institucional para las respuestas (BE-01 §4.4). Los DTO de respuesta usan
 * siempre {@code OffsetDateTime}; Jackson los serializa como ISO-8601 con
 * desplazamiento, por ejemplo {@code 2026-08-31T18:00:00-05:00}.
 */
@Component
public class ConversorFechas {

    private final Clock reloj;

    public ConversorFechas(Clock reloj) {
        this.reloj = reloj;
    }

    public OffsetDateTime aRespuesta(Instant instante) {
        return instante == null ? null : instante.atZone(reloj.getZone()).toOffsetDateTime();
    }
}
