package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.EstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.EventoEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.HistorialEstadoImplemento;
import co.edu.itm.prestamosdeportivos.dominio.ImplementoDeportivo;
import co.edu.itm.prestamosdeportivos.dominio.Rol;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.HistorialEstadoImplementoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import co.edu.itm.prestamosdeportivos.seguridad.UsuarioAutenticado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioServicio {

    private final ImplementoDeportivoRepositorio implementos;
    private final CategoriaRepositorio categorias;
    private final HistorialEstadoImplementoRepositorio historial;
    private final UsuarioRepositorio usuarios;

    public InventarioServicio(ImplementoDeportivoRepositorio implementos, CategoriaRepositorio categorias,
            HistorialEstadoImplementoRepositorio historial, UsuarioRepositorio usuarios) {
        this.implementos = implementos;
        this.categorias = categorias;
        this.historial = historial;
        this.usuarios = usuarios;
    }

    @Transactional
    public ImplementoDeportivo crear(String codigo, String nombre, String descripcion, UUID categoriaId,
            EstadoImplemento estado) {
        validarUnicoCodigo(codigo, null);
        EstadoImplemento inicial = estado == null ? EstadoImplemento.DISPONIBLE : estado;
        if (inicial != EstadoImplemento.DISPONIBLE && inicial != EstadoImplemento.EN_MANTENIMIENTO) {
            throw new ExcepcionDominio(CodigoError.VALIDACION_FALLIDA, "El estado inicial debe ser DISPONIBLE o EN_MANTENIMIENTO.",
                    List.of(new ErrorCampo("estado", "no es válido para una unidad nueva")));
        }
        Categoria categoria = obtenerCategoria(categoriaId);
        categoria.exigirActivaParaNuevaUnidad();
        return implementos.save(new ImplementoDeportivo(codigo.trim(), nombre.trim(), limpiar(descripcion), categoria, inicial));
    }

    @Transactional(readOnly = true)
    public Page<ImplementoDeportivo> listar(String q, UUID categoriaId, EstadoImplemento estado, Pageable pageable) {
        return implementos.findAll(InventarioEspecificaciones.filtros(q, categoriaId, estado), determinista(pageable));
    }

    @Transactional(readOnly = true)
    public ImplementoDeportivo obtener(UUID id) {
        return implementos.findById(id).orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO));
    }

    @Transactional
    public ImplementoDeportivo actualizar(UUID id, String codigo, String nombre, String descripcion, UUID categoriaId) {
        ImplementoDeportivo implemento = obtener(id);
        if (codigo == null && nombre == null && descripcion == null && categoriaId == null) {
            throw new ExcepcionDominio(CodigoError.VALIDACION_FALLIDA, "Debe enviar al menos un campo para actualizar.",
                    List.of(new ErrorCampo("body", "no contiene campos permitidos")));
        }
        if (codigo != null && codigo.isBlank()) {
            throw campoRequerido("codigoInventario");
        }
        if (nombre != null && nombre.isBlank()) {
            throw campoRequerido("nombre");
        }
        String nuevoCodigo = codigo == null ? implemento.getCodigoInventario() : codigo.trim();
        String nuevoNombre = nombre == null ? implemento.getNombre() : nombre.trim();
        String nuevaDescripcion = descripcion == null ? implemento.getDescripcion() : limpiar(descripcion);
        Categoria nuevaCategoria = categoriaId == null ? implemento.getCategoria() : obtenerCategoria(categoriaId);
        if (categoriaId != null && !categoriaId.equals(implemento.getCategoria().getId())) {
            nuevaCategoria.exigirActivaParaNuevaUnidad();
        }
        validarUnicoCodigo(nuevoCodigo, id);
        implemento.actualizar(nuevoCodigo, nuevoNombre, nuevaDescripcion, nuevaCategoria);
        return implemento;
    }

    @Transactional
    public ImplementoDeportivo cambiarEstado(UUID id, EstadoImplemento nuevoEstado, String motivo) {
        return cambiarEstado(id, nuevoEstado, motivo, null);
    }

    @Transactional
    public ImplementoDeportivo cambiarEstado(UUID id, EstadoImplemento nuevoEstado, String motivo,
            UsuarioAutenticado operador) {
        ImplementoDeportivo implemento = obtener(id);
        EstadoImplemento anterior = implemento.getEstado();
        implemento.cambiarEstado(nuevoEstado, EventoEstadoImplemento.ADMINISTRACION);
        Usuario operadorEntidad = operador == null ? null : usuarios.findByIdAndRol(operador.id(), Rol.ADMIN)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ExcepcionDominio(CodigoError.SESION_INVALIDA));
        historial.save(new HistorialEstadoImplemento(implemento, anterior, nuevoEstado,
                EventoEstadoImplemento.ADMINISTRACION, motivo.trim(), operadorEntidad));
        return implemento;
    }

    private Categoria obtenerCategoria(UUID id) {
        return categorias.findById(id).orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO,
                "La categoría indicada no existe.", List.of(new ErrorCampo("categoriaId", "no existe"))));
    }

    private void validarUnicoCodigo(String codigo, UUID excluido) {
        implementos.findByCodigoInventario(codigo.trim()).filter(unidad -> !unidad.getId().equals(excluido))
                .ifPresent(unidad -> {
                    throw new ExcepcionDominio(CodigoError.CONFLICTO_UNICIDAD,
                            "Ya existe una unidad con ese código de inventario.",
                            List.of(new ErrorCampo("codigoInventario", "ya está registrado")));
                });
    }

    private static Pageable determinista(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by("codigoInventario").ascending();
        }
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id").ascending());
        }
        return org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static ExcepcionDominio campoRequerido(String campo) {
        return new ExcepcionDominio(CodigoError.VALIDACION_FALLIDA, "El valor no puede estar vacío.",
                List.of(new ErrorCampo(campo, "es obligatorio cuando se envía")));
    }
}
