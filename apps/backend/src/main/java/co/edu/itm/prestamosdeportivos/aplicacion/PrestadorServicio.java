package co.edu.itm.prestamosdeportivos.aplicacion;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;

import co.edu.itm.prestamosdeportivos.dominio.EstadoPrestador;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioEspecificaciones;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.web.ActualizarPrestadorSolicitud;
import co.edu.itm.prestamosdeportivos.web.CambiarEstadoPrestadorSolicitud;
import co.edu.itm.prestamosdeportivos.web.PaginaRespuesta;
import co.edu.itm.prestamosdeportivos.web.PrestadorRespuesta;
import co.edu.itm.prestamosdeportivos.web.RegistrarPrestadorSolicitud;
import co.edu.itm.prestamosdeportivos.web.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.web.error.ExcepcionApi;

@Service
public class PrestadorServicio {

    private static final List<String> CAMPOS_ORDEN = List.of(
            "nombres", "apellidos", "identificacion", "correo", "estado", "creadoEn");

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;
    private final ZoneId zonaHoraria;

    public PrestadorServicio(UsuarioRepositorio usuarios, PasswordEncoder passwordEncoder, Clock reloj,
            @Value("${app.zona-horaria}") String zonaHoraria) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
        this.zonaHoraria = ZoneId.of(zonaHoraria);
    }

    @Transactional
    public PrestadorRespuesta registrar(RegistrarPrestadorSolicitud solicitud) {
        String identificacion = solicitud.identificacion().trim();
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        validarUnicidad(identificacion, correo, null);
        Usuario usuario = new Usuario(identificacion, solicitud.nombres().trim(), solicitud.apellidos().trim(),
                correo, passwordEncoder.encode(solicitud.contrasenaInicial()), Rol.PRESTADOR, true,
                reloj.instant());
        try {
            return representar(usuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
            throw conflictoUnicidad(ex);
        }
    }

    @Transactional(readOnly = true)
    public PaginaRespuesta<PrestadorRespuesta> listar(int pagina, int tamano, String orden, String q,
            String nombres, String identificacion, String correo, String estado) {
        if (pagina < 0 || tamano < 1 || tamano > 100) {
            throw solicitudInvalida("pagina debe ser mayor o igual a 0 y tamano debe estar entre 1 y 100.",
                    "pagina");
        }
        EstadoPrestador filtroEstado = parsearEstado(estado);
        Pageable pageable = PageRequest.of(pagina, tamano, parsearOrden(orden));
        Specification<Usuario> especificacion = UsuarioEspecificaciones.prestadores(
                q, nombres, identificacion, correo, filtroEstado);
        Page<Usuario> resultado = usuarios.findAll(especificacion, pageable);
        return new PaginaRespuesta<>(resultado.getContent().stream().map(this::representar).toList(),
                resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PrestadorRespuesta consultar(UUID id) {
        return representar(buscarPrestador(id));
    }

    @Transactional
    public PrestadorRespuesta actualizar(UUID id, ActualizarPrestadorSolicitud solicitud) {
        if (solicitud.vacia()) {
            throw solicitudInvalida("Debe indicar al menos un dato permitido para actualizar.", "solicitud");
        }
        Usuario usuario = buscarPrestador(id);
        String nombres = actualizarTexto(solicitud.nombres(), usuario.getNombres(), "nombres");
        String apellidos = actualizarTexto(solicitud.apellidos(), usuario.getApellidos(), "apellidos");
        String identificacion = actualizarTexto(solicitud.identificacion(), usuario.getIdentificacion(), "identificacion");
        String correo = solicitud.correo() == null ? usuario.getCorreo()
                : actualizarTexto(solicitud.correo().trim().toLowerCase(Locale.ROOT), usuario.getCorreo(), "correo");
        validarUnicidad(identificacion, correo, id);
        usuario.actualizarPrestador(nombres, apellidos, identificacion, correo, reloj.instant());
        try {
            return representar(usuarios.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
            throw conflictoUnicidad(ex);
        }
    }

    @Transactional
    public PrestadorRespuesta cambiarEstado(UUID id, CambiarEstadoPrestadorSolicitud solicitud) {
        Usuario usuario = buscarPrestador(id);
        usuario.cambiarEstado(solicitud.estado() == EstadoPrestador.ACTIVO, reloj.instant());
        return representar(usuarios.saveAndFlush(usuario));
    }

    private Usuario buscarPrestador(UUID id) {
        return usuarios.findByIdAndRol(id, Rol.PRESTADOR)
                .orElseThrow(() -> new ExcepcionApi(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO",
                        "Recurso no encontrado", "No existe un prestador con el identificador indicado."));
    }

    private void validarUnicidad(String identificacion, String correo, UUID id) {
        boolean identificacionDuplicada = id == null
                ? usuarios.existsByIdentificacion(identificacion)
                : usuarios.existsByIdentificacionAndIdNot(identificacion, id);
        boolean correoDuplicado = id == null
                ? usuarios.existsByCorreoIgnoreCase(correo)
                : usuarios.existsByCorreoIgnoreCaseAndIdNot(correo, id);
        List<ErrorCampo> errores = new ArrayList<>();
        if (identificacionDuplicada) {
            errores.add(new ErrorCampo("identificacion", "Ya existe un usuario con esta identificación."));
        }
        if (correoDuplicado) {
            errores.add(new ErrorCampo("correo", "Ya existe un usuario con este correo."));
        }
        if (!errores.isEmpty()) {
            throw new ExcepcionApi(HttpStatus.CONFLICT, "CONFLICTO_UNICIDAD", "Conflicto de unicidad",
                    "La identificación o el correo ya están registrados.", errores);
        }
    }

    private ExcepcionApi conflictoUnicidad(DataIntegrityViolationException ex) {
        String restriccion = buscarRestriccion(ex);
        List<ErrorCampo> errores = new ArrayList<>();
        if (restriccion != null && restriccion.contains("identificacion")) {
            errores.add(new ErrorCampo("identificacion", "Ya existe un usuario con esta identificación."));
        }
        if (restriccion != null && restriccion.contains("correo")) {
            errores.add(new ErrorCampo("correo", "Ya existe un usuario con este correo."));
        }
        if (errores.isEmpty()) {
            errores.add(new ErrorCampo("identificacion", "Puede existir un registro con esta identificación."));
            errores.add(new ErrorCampo("correo", "Puede existir un registro con este correo."));
        }
        return new ExcepcionApi(HttpStatus.CONFLICT, "CONFLICTO_UNICIDAD", "Conflicto de unicidad",
                "La identificación o el correo ya están registrados.", errores);
    }

    private String buscarRestriccion(Throwable error) {
        Throwable actual = error;
        while (actual != null) {
            if (actual instanceof ConstraintViolationException violacion) {
                return violacion.getConstraintName();
            }
            actual = actual.getCause();
        }
        return null;
    }

    private Sort parsearOrden(String orden) {
        if (orden == null || orden.isBlank()) {
            return Sort.by(Sort.Order.asc("nombres"), Sort.Order.asc("id"));
        }
        String[] partes = orden.split(",", -1);
        if (partes.length != 2 || !CAMPOS_ORDEN.contains(partes[0])) {
            throw solicitudInvalida("El campo de orden no está permitido.", "orden");
        }
        Sort.Direction direccion;
        try {
            direccion = Sort.Direction.fromString(partes[1]);
        } catch (IllegalArgumentException ex) {
            throw solicitudInvalida("La dirección de orden debe ser asc o desc.", "orden");
        }
        String propiedad = partes[0].equals("estado") ? "activo"
                : partes[0].equals("creadoEn") ? "creadoEn" : partes[0];
        Sort sort = Sort.by(direccion, propiedad);
        if (!propiedad.equals("id")) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return sort;
    }

    private EstadoPrestador parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoPrestador.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw solicitudInvalida("El estado debe ser ACTIVO o INACTIVO.", "estado");
        }
    }

    private String actualizarTexto(String valor, String actual, String campo) {
        if (valor == null) {
            return actual;
        }
        if (valor.isBlank()) {
            throw solicitudInvalida("El campo no puede quedar vacío.", campo);
        }
        return valor.trim();
    }

    private ExcepcionApi solicitudInvalida(String detalle, String campo) {
        return new ExcepcionApi(HttpStatus.BAD_REQUEST, "VALIDACION_FALLIDA", "Solicitud inválida", detalle,
                List.of(new ErrorCampo(campo, detalle)));
    }

    private PrestadorRespuesta representar(Usuario usuario) {
        return new PrestadorRespuesta(usuario.getId(), usuario.getNombres(), usuario.getApellidos(),
                usuario.getIdentificacion(), usuario.getCorreo(), usuario.getRol().name(),
                usuario.isActivo() ? EstadoPrestador.ACTIVO.name() : EstadoPrestador.INACTIVO.name(),
                usuario.getCreadoEn().atZone(zonaHoraria).toOffsetDateTime(),
                usuario.getActualizadoEn().atZone(zonaHoraria).toOffsetDateTime());
    }
}
