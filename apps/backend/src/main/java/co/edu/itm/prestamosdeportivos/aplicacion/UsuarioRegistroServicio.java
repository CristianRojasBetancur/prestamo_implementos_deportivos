package co.edu.itm.prestamosdeportivos.aplicacion;

import java.time.Clock;
import java.util.List;

import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.web.dto.UsuarioDto.CrearSolicitanteSolicitud;
import co.edu.itm.prestamosdeportivos.web.dto.UsuarioDto.SolicitanteRespuesta;
import co.edu.itm.prestamosdeportivos.web.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.web.error.ExcepcionApi;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta operativa de solicitantes USER durante la preparación de un préstamo. */
@Service
public class UsuarioRegistroServicio {

    private final UsuarioRepositorio usuarios;
    private final Clock reloj;

    public UsuarioRegistroServicio(UsuarioRepositorio usuarios, Clock reloj) {
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    @Transactional
    public SolicitanteRespuesta registrar(CrearSolicitanteSolicitud solicitud) {
        String identificacion = solicitud.identificacion().trim();
        String nombres = solicitud.nombres().trim();
        String apellidos = solicitud.apellidos().trim();
        if (usuarios.existsByIdentificacion(identificacion)) {
            throw conflictoIdentificacion();
        }
        Usuario usuario = new Usuario(identificacion, nombres, apellidos, null, null, Rol.USER, true,
                reloj.instant());
        try {
            Usuario creado = usuarios.saveAndFlush(usuario);
            return new SolicitanteRespuesta(creado.getId(), creado.getNombres(), creado.getApellidos(),
                    creado.getIdentificacion(), creado.isActivo());
        } catch (DataIntegrityViolationException ex) {
            throw conflictoIdentificacion();
        }
    }

    private ExcepcionApi conflictoIdentificacion() {
        return new ExcepcionApi(HttpStatus.CONFLICT, "CONFLICTO_UNICIDAD", "Registro duplicado",
                "Ya existe un usuario con esa identificación.",
                List.of(new ErrorCampo("identificacion", "Ya existe un usuario con esta identificación.")));
    }
}
