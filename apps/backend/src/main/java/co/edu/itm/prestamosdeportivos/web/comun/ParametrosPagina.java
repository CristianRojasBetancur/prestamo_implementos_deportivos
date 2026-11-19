package co.edu.itm.prestamosdeportivos.web.comun;

import java.util.List;
import java.util.Set;

import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Parámetros de paginación de los listados (BE-01 §4.5). En los controladores
 * se reciben como {@code @Valid @ParameterObject ParametrosPagina pagina} y se
 * convierten con {@link #aPageable(Set, Sort)}, que restringe los campos de
 * orden a los permitidos por cada recurso.
 */
public record ParametrosPagina(
        @Schema(description = "Número de página, desde 0.", example = "0", defaultValue = "0")
        @Min(value = 0, message = "debe ser mayor o igual a 0")
        Integer pagina,
        @Schema(description = "Elementos por página (1-100).", example = "20", defaultValue = "20")
        @Min(value = 1, message = "debe estar entre 1 y 100")
        @Max(value = 100, message = "debe estar entre 1 y 100")
        Integer tamano,
        @Schema(description = "Orden en formato campo,asc o campo,desc.", example = "fechaPrestamo,desc")
        @Pattern(regexp = "^[A-Za-z]+,(asc|desc)$", message = "debe tener el formato campo,asc o campo,desc")
        String orden) {

    public static final int PAGINA_POR_DEFECTO = 0;
    public static final int TAMANO_POR_DEFECTO = 20;

    public Pageable aPageable(Set<String> camposPermitidos, Sort ordenPorDefecto) {
        int numero = pagina != null ? pagina : PAGINA_POR_DEFECTO;
        int cantidad = tamano != null ? tamano : TAMANO_POR_DEFECTO;
        if (orden == null || orden.isBlank()) {
            return PageRequest.of(numero, cantidad, ordenPorDefecto);
        }
        String[] partes = orden.split(",");
        if (!camposPermitidos.contains(partes[0])) {
            throw new ExcepcionDominio(CodigoError.VALIDACION_FALLIDA, CodigoError.VALIDACION_FALLIDA.detallePorDefecto(),
                    List.of(new ErrorCampo("orden", "solo admite: " + String.join(", ", camposPermitidos.stream().sorted().toList()))));
        }
        return PageRequest.of(numero, cantidad, Sort.by(Sort.Direction.fromString(partes[1]), partes[0]));
    }
}
