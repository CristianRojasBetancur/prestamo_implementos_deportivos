package co.edu.itm.prestamosdeportivos.web;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import co.edu.itm.prestamosdeportivos.aplicacion.PrestamoServicio;
import co.edu.itm.prestamosdeportivos.aplicacion.PrestamoConsultaServicio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.CrearPrestamoSolicitud;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.PrestamoRespuesta;
import co.edu.itm.prestamosdeportivos.web.dto.PrestamoDto.PrestamoResumenRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** CU-14: entrega de unidades al solicitante. */
@RestController
@RequestMapping("/api/v1")
public class PrestamoControlador {
    private final PrestamoServicio servicio;
    private final PrestamoConsultaServicio consulta;
    public PrestamoControlador(PrestamoServicio servicio, PrestamoConsultaServicio consulta) {
        this.servicio = servicio;
        this.consulta = consulta;
    }

    @GetMapping("/prestamos")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Consultar préstamos", description = "Lista préstamos con búsqueda, filtros, paginación y estado calculado sin mutar datos.")
    public PaginaRespuesta<PrestamoResumenRespuesta> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden,
            @Parameter(description = "Uno o varios estados separados por coma.", example = "VIGENTE,VENCIDO")
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) String identificacion,
            @RequestParam(required = false) OffsetDateTime desde,
            @RequestParam(required = false) OffsetDateTime hasta,
            @RequestParam(required = false, name = "q") String texto) {
        return consulta.listar(pagina, tamano, orden, estado, usuarioId, identificacion, desde, hasta, texto);
    }

    @GetMapping("/prestamos/{id}")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Consultar detalle de préstamo", description = "Incluye usuario, unidades, fechas, estado, devoluciones e incidencias relacionadas.")
    public PrestamoRespuesta consultar(@PathVariable UUID id) {
        return consulta.consultar(id);
    }

    @PostMapping("/prestamos")
    @PreAuthorize("hasRole('PRESTADOR')")
    @Operation(summary = "Registrar un préstamo", description = "Registra atómicamente la entrega y bloquea las unidades solicitadas.")
    public ResponseEntity<PrestamoRespuesta> registrar(@Valid @RequestBody CrearPrestamoSolicitud solicitud,
            @AuthenticationPrincipal UsuarioAutenticado operador) {
        var respuesta = servicio.registrar(solicitud.usuarioId(), solicitud.implementoIds(), operador);
        return ResponseEntity.created(URI.create("/api/v1/prestamos/" + respuesta.id())).body(respuesta);
    }
}
