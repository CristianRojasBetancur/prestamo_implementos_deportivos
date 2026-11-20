package co.edu.itm.prestamosdeportivos.web;

import java.util.UUID;

import co.edu.itm.prestamosdeportivos.aplicacion.DisponibilidadServicio;
import co.edu.itm.prestamosdeportivos.persistencia.DisponibilidadProyeccion;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import co.edu.itm.prestamosdeportivos.web.dto.CatalogoDto.DisponibilidadRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaCategoria;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/** CU-12 público de BE-01; la ruta inglesa es alias solicitado por BE-08. */
@RestController
@Validated
@RequestMapping("/api/v1")
public class DisponibilidadControlador {

    private final DisponibilidadServicio servicio;

    public DisponibilidadControlador(DisponibilidadServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping({"/availability", "/disponibilidad"})
    @SecurityRequirements
    @Operation(summary = "Consultar disponibilidad", description = "Catálogo agregado público; no expone códigos ni UUID de unidades físicas.")
    public PaginaRespuesta<DisponibilidadRespuesta> consultar(
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @Size(max = 80, message = "no puede superar 80 caracteres") String q,
            @Valid @ParameterObject ParametrosPagina pagina) {
        if (categoriaId != null && categoryId != null && !categoriaId.equals(categoryId)) {
            throw new ExcepcionDominio(CodigoError.VALIDACION_FALLIDA,
                    "Use un único valor para el filtro de categoría.",
                    java.util.List.of(new ErrorCampo("categoryId", "conflicta con categoriaId")));
        }
        UUID filtroCategoria = categoriaId != null ? categoriaId : categoryId;
        var pageable = pagina.aPageable(servicio.camposOrdenPermitidos(), Sort.by("nombre").ascending());
        return PaginaRespuesta.de(servicio.consultar(q, filtroCategoria, pageable), DisponibilidadControlador::respuesta);
    }

    private static DisponibilidadRespuesta respuesta(DisponibilidadProyeccion fila) {
        return new DisponibilidadRespuesta(fila.getNombre(),
                new ReferenciaCategoria(fila.getCategoriaId(), fila.getCategoriaNombre()),
                fila.getUnidadesDisponibles(), fila.getUnidadesTotales(), fila.getDisponible());
    }
}
