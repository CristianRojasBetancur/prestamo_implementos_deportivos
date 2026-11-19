package co.edu.itm.prestamosdeportivos.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestamo;
import co.edu.itm.prestamosdeportivos.dominio.TipoIncidencia;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Referencias resumidas a otros recursos, reutilizadas dentro de las respuestas. */
public final class Referencias {

    private Referencias() {
    }

    /** Usuario o prestador. {@code identificacion} solo se incluye cuando el caso de uso la muestra. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReferenciaUsuario(UUID id, String nombres, String apellidos, String identificacion) {
    }

    public record ReferenciaCategoria(UUID id, String nombre) {
    }

    /** Unidad física. {@code estado} solo se incluye cuando el caso de uso lo muestra. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReferenciaImplemento(UUID id, String codigoInventario, String nombre, EstadoImplemento estado) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReferenciaPrestamo(UUID id, String codigo, EstadoPrestamo estado) {
    }

    public record ReferenciaDevolucion(UUID id, String codigo, OffsetDateTime fechaDevolucion) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReferenciaIncidencia(UUID id, String codigo, TipoIncidencia tipo, String codigoInventario) {
    }
}
