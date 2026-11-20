package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioEspecificaciones;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.web.comun.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.comun.ParametrosPagina;
import co.edu.itm.prestamosdeportivos.web.dto.UsuarioDto.SolicitanteRespuesta;

/** Consulta de solicitantes para la preparación y validación de un préstamo. */
@Service
public class UsuarioConsultaServicio {

    private static final Set<String> CAMPOS_ORDEN = Set.of("nombres", "apellidos", "identificacion", "activo",
            "creadoEn");

    private final UsuarioRepositorio usuarios;

    public UsuarioConsultaServicio(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public PaginaRespuesta<SolicitanteRespuesta> listar(String texto, ParametrosPagina pagina) {
        var pageable = pagina.aPageable(CAMPOS_ORDEN,
                Sort.by(Sort.Order.asc("nombres"), Sort.Order.asc("apellidos"), Sort.Order.asc("id")));
        Page<Usuario> resultado = usuarios.findAll(UsuarioEspecificaciones.solicitantes(texto), pageable);
        return PaginaRespuesta.de(resultado, usuario -> new SolicitanteRespuesta(usuario.getId(), usuario.getNombres(),
                usuario.getApellidos(), usuario.getIdentificacion(), usuario.isActivo()));
    }
}
