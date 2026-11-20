package co.edu.itm.prestamosdeportivos.web;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.aplicacion.InventarioServicio;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import co.edu.itm.prestamosdeportivos.web.dto.GestionInventarioDto.ActualizarUnidadSolicitud;
import co.edu.itm.prestamosdeportivos.web.dto.GestionInventarioDto.CambioEstadoSolicitud;
import co.edu.itm.prestamosdeportivos.web.dto.GestionInventarioDto.CrearUnidadSolicitud;
import co.edu.itm.prestamosdeportivos.web.dto.GestionInventarioDto.UnidadRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.Referencias.ReferenciaCategoria;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/v1/inventory/units")
@PreAuthorize("hasRole('ADMIN')")
public class InventarioControlador {

    private static final Set<String> CAMPOS_ORDEN = Set.of("codigoInventario", "nombre", "estado", "creadoEn", "id");
    private final InventarioServicio servicio;

    public InventarioControlador(InventarioServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<UnidadRespuesta> crear(@Valid @RequestBody CrearUnidadSolicitud solicitud) {
        ImplementoDeportivo creada = servicio.crear(solicitud.codigoInventario(), solicitud.nombre(),
                solicitud.descripcion(), solicitud.categoriaId(), solicitud.estado());
        return ResponseEntity.created(URI.create("/api/v1/inventory/units/" + creada.getId())).body(respuesta(creada));
    }

    @GetMapping
    public PaginaRespuesta<UnidadRespuesta> listar(@RequestParam(required = false) @Size(max = 80, message = "no puede superar 80 caracteres") String q,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) EstadoImplemento estado,
            @Valid @ParameterObject ParametrosPagina pagina) {
        var pageable = pagina.aPageable(CAMPOS_ORDEN, Sort.by("codigoInventario").ascending());
        Page<ImplementoDeportivo> resultado = servicio.listar(q, categoriaId, estado, pageable);
        return PaginaRespuesta.de(resultado, InventarioControlador::respuesta);
    }

    @GetMapping("/{id}")
    public UnidadRespuesta obtener(@PathVariable UUID id) {
        return respuesta(servicio.obtener(id));
    }

    @PatchMapping("/{id}")
    public UnidadRespuesta actualizar(@PathVariable UUID id, @Valid @RequestBody ActualizarUnidadSolicitud solicitud) {
        return respuesta(servicio.actualizar(id, solicitud.codigoInventario(), solicitud.nombre(),
                solicitud.descripcion(), solicitud.categoriaId()));
    }

    @PatchMapping("/{id}/state")
    public UnidadRespuesta cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambioEstadoSolicitud solicitud,
            @AuthenticationPrincipal UsuarioAutenticado operador) {
        return respuesta(servicio.cambiarEstado(id, solicitud.estado(), solicitud.motivo(), operador));
    }

    private static UnidadRespuesta respuesta(ImplementoDeportivo implemento) {
        return new UnidadRespuesta(implemento.getId(), implemento.getCodigoInventario(), implemento.getNombre(),
                implemento.getDescripcion(), new ReferenciaCategoria(implemento.getCategoria().getId(),
                        implemento.getCategoria().getNombre()),
                implemento.getEstado(), implemento.esPrestable(), implemento.getCreadoEn(), implemento.getActualizadoEn());
    }
}
