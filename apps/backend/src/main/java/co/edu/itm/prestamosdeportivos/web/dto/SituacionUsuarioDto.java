package co.edu.itm.prestamosdeportivos.web.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;

public final class SituacionUsuarioDto {
    private SituacionUsuarioDto() {}
    public record Usuario(UUID id, String nombres, String apellidos, String identificacion) {}
    public record Motivo(CodigoError codigo, String mensaje) {}
    public record Sancion(OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {}
    public record Respuesta(Usuario usuario, boolean habilitado, List<Motivo> motivos, long unidadesActivas,
            int unidadesMaximas, long unidadesDisponiblesParaPrestar, long prestamosActivos,
            long prestamosVencidos, Sancion sancionVigente) {}
}
