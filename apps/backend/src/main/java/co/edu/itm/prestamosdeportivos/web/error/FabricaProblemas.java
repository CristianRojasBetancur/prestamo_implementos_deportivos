package co.edu.itm.prestamosdeportivos.web.error;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import co.edu.itm.prestamosdeportivos.configuracion.FiltroCorrelacion;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Construye respuestas {@link ProblemaRespuesta} coherentes para toda la API. */
@Component
public class FabricaProblemas {

    static final String URI_BASE_TIPOS = "https://prestamos.itm.edu.co/errores/";

    private final Clock reloj;

    public FabricaProblemas(Clock reloj) {
        this.reloj = reloj;
    }

    public ProblemaRespuesta crear(HttpStatusCode estado, CodigoError codigo, String detalle, String ruta,
            List<ErrorCampo> errores) {
        String marcaTiempo = OffsetDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new ProblemaRespuesta(
                URI_BASE_TIPOS + codigo.slug(),
                codigo.titulo(),
                estado.value(),
                detalle != null ? detalle : codigo.detallePorDefecto(),
                ruta,
                codigo.name(),
                marcaTiempo,
                MDC.get(FiltroCorrelacion.CLAVE_MDC),
                errores == null || errores.isEmpty() ? null : List.copyOf(errores));
    }

    public ResponseEntity<Object> respuesta(ProblemaRespuesta problema) {
        return ResponseEntity.status(problema.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }
}
