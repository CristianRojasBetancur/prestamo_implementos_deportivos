package co.edu.itm.prestamosdeportivos.web;

import java.util.Set;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.aplicacion.InventarioServicio;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.web.comun.ConversorFechas;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import co.edu.itm.prestamosdeportivos.web.dto.InventarioDto.ImplementoRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaCategoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/** CU-14: selección de unidades físicas para registrar un préstamo. */
@RestController
@Validated
@RequestMapping("/api/v1/implementos")
@PreAuthorize("hasRole('PRESTADOR')")
public class ImplementosPrestablesControlador {

    private static final Set<String> CAMPOS_ORDEN = Set.of("codigoInventario", "nombre", "estado", "id");

    private final InventarioServicio servicio;
    private final ConversorFechas fechas;

    public ImplementosPrestablesControlador(InventarioServicio servicio, ConversorFechas fechas) {
        this.servicio = servicio;
        this.fechas = fechas;
    }

    @GetMapping("/prestables")
    public PaginaRespuesta<ImplementoRespuesta> listar(
            @RequestParam(required = false) @Size(max = 80, message = "no puede superar 80 caracteres") String q,
            @RequestParam(required = false) UUID categoriaId,
            @Valid @ParameterObject ParametrosPagina pagina) {
        var pageable = pagina.aPageable(CAMPOS_ORDEN, Sort.by("codigoInventario").ascending());
        return PaginaRespuesta.de(servicio.listarPrestables(q, categoriaId, pageable), this::respuesta);
    }

    private ImplementoRespuesta respuesta(ImplementoDeportivo implemento) {
        return new ImplementoRespuesta(implemento.getId(), implemento.getCodigoInventario(), implemento.getNombre(),
                implemento.getDescripcion(), new ReferenciaCategoria(implemento.getCategoria().getId(),
                        implemento.getCategoria().getNombre()), implemento.getEstado(), implemento.esPrestable(),
                fechas.aRespuesta(implemento.getCreadoEn()));
    }
}
