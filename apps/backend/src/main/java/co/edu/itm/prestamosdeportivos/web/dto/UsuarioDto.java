package co.edu.itm.prestamosdeportivos.web.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaUsuario;

/** CU-13: búsqueda de solicitantes y situación de préstamo (BE-01 §10.5). */
public final class UsuarioDto {

    private UsuarioDto() {
    }

    public record SolicitanteRespuesta(UUID id, String nombres, String apellidos, String identificacion,
            boolean activo) {
    }

    public record MotivoInhabilitacion(CodigoError codigo, String mensaje) {
    }

    public record SancionVigenteRespuesta(OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
    }

    public record SituacionUsuarioRespuesta(ReferenciaUsuario usuario, boolean habilitado,
            List<MotivoInhabilitacion> motivos, int unidadesActivas, int unidadesMaximas,
            int unidadesDisponiblesParaPrestar, int prestamosActivos, int prestamosVencidos,
            SancionVigenteRespuesta sancionVigente) {
    }
}
