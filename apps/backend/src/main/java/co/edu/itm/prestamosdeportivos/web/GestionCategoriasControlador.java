package co.edu.itm.prestamosdeportivos.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.aplicacion.CategoriaServicio;
import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.web.dto.GestionInventarioDto.CategoriaRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.GestionInventarioDto.CategoriaSolicitud;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categorias")
public class GestionCategoriasControlador {

    private final CategoriaServicio servicio;

    public GestionCategoriasControlador(CategoriaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CategoriaRespuesta> listar() {
        return servicio.listar().stream().map(GestionCategoriasControlador::respuesta).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoriaRespuesta obtener(@PathVariable UUID id) {
        return respuesta(servicio.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaRespuesta> crear(@Valid @RequestBody CategoriaSolicitud solicitud) {
        Categoria creada = servicio.crear(solicitud.nombre(), solicitud.descripcion(), solicitud.activa());
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + creada.getId())).body(respuesta(creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoriaRespuesta actualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaSolicitud solicitud) {
        return respuesta(servicio.actualizar(id, solicitud.nombre(), solicitud.descripcion(), solicitud.activa()));
    }

    private static CategoriaRespuesta respuesta(Categoria categoria) {
        return new CategoriaRespuesta(categoria.getId(), categoria.getNombre(), categoria.getDescripcion(),
                categoria.isActiva(), categoria.getCreadoEn(), categoria.getActualizadoEn());
    }
}
