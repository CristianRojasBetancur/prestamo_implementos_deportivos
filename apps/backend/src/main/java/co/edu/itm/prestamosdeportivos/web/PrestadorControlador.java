package co.edu.itm.prestamosdeportivos.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.itm.prestamosdeportivos.aplicacion.PrestadorServicio;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/v1/providers")
@PreAuthorize("hasRole('ADMIN')")
public class PrestadorControlador {

    private final PrestadorServicio prestadores;

    public PrestadorControlador(PrestadorServicio prestadores) {
        this.prestadores = prestadores;
    }

    @PostMapping
    public ResponseEntity<PrestadorRespuesta> registrar(@Valid @RequestBody RegistrarPrestadorSolicitud solicitud) {
        PrestadorRespuesta respuesta = prestadores.registrar(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/providers/" + respuesta.id())).body(respuesta);
    }

    @GetMapping
    public PaginaRespuesta<PrestadorRespuesta> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String identificacion,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String estado) {
        return prestadores.listar(pagina, tamano, orden, q, nombres, identificacion, correo, estado);
    }

    @GetMapping("/{id}")
    public PrestadorRespuesta consultar(@PathVariable UUID id) {
        return prestadores.consultar(id);
    }

    @PatchMapping("/{id}")
    public PrestadorRespuesta actualizar(@PathVariable UUID id,
            @Valid @RequestBody ActualizarPrestadorSolicitud solicitud) {
        return prestadores.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    public PrestadorRespuesta cambiarEstado(@PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoPrestadorSolicitud solicitud) {
        return prestadores.cambiarEstado(id, solicitud);
    }
}
