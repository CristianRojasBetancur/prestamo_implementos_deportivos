package co.edu.itm.prestamosdeportivos.dominio.error;

import java.util.List;
import java.util.Objects;

/**
 * Excepción de negocio con un {@link CodigoError} estable. Su mensaje se
 * muestra tal cual a la persona usuaria (campo {@code detail}), por lo que debe
 * redactarse en español claro y nunca incluir datos internos.
 */
public class ExcepcionDominio extends RuntimeException {

    private final CodigoError codigo;
    private final List<ErrorCampo> errores;

    public ExcepcionDominio(CodigoError codigo) {
        this(codigo, codigo.detallePorDefecto(), List.of());
    }

    public ExcepcionDominio(CodigoError codigo, String detalle) {
        this(codigo, detalle, List.of());
    }

    public ExcepcionDominio(CodigoError codigo, String detalle, List<ErrorCampo> errores) {
        super(Objects.requireNonNull(detalle, "detalle"));
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.errores = List.copyOf(errores);
    }

    public CodigoError getCodigo() {
        return codigo;
    }

    public List<ErrorCampo> getErrores() {
        return errores;
    }
}
