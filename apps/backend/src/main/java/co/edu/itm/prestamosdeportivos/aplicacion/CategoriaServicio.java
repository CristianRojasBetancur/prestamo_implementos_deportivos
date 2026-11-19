package co.edu.itm.prestamosdeportivos.aplicacion;

import java.util.List;
import java.util.UUID;

import co.edu.itm.prestamosdeportivos.dominio.Categoria;
import co.edu.itm.prestamosdeportivos.dominio.error.CodigoError;
import co.edu.itm.prestamosdeportivos.dominio.error.ErrorCampo;
import co.edu.itm.prestamosdeportivos.dominio.error.ExcepcionDominio;
import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaServicio {

    private final CategoriaRepositorio categorias;

    public CategoriaServicio(CategoriaRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return categorias.findByActivaTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Categoria obtener(UUID id) {
        return categorias.findById(id).orElseThrow(() -> new ExcepcionDominio(CodigoError.RECURSO_NO_ENCONTRADO));
    }

    @Transactional
    public Categoria crear(String nombre, String descripcion, Boolean activa) {
        String limpio = limpiar(nombre);
        if (categorias.buscarPorNombre(limpio).isPresent()) {
            throw duplicado();
        }
        Categoria categoria = new Categoria(limpio, limpiarOpcional(descripcion));
        if (Boolean.FALSE.equals(activa)) {
            categoria.actualizar(limpio, limpiarOpcional(descripcion), false);
        }
        return categorias.save(categoria);
    }

    @Transactional
    public Categoria actualizar(UUID id, String nombre, String descripcion, Boolean activa) {
        Categoria categoria = obtener(id);
        String limpio = limpiar(nombre);
        categorias.buscarPorNombre(limpio).filter(otra -> !otra.getId().equals(id)).ifPresent(otra -> {
            throw duplicado();
        });
        categoria.actualizar(limpio, limpiarOpcional(descripcion), activa == null ? categoria.isActiva() : activa);
        return categoria;
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private static String limpiarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static ExcepcionDominio duplicado() {
        return new ExcepcionDominio(CodigoError.CONFLICTO_UNICIDAD, "Ya existe una categoría con ese nombre.",
                List.of(new ErrorCampo("nombre", "ya está registrado")));
    }
}
