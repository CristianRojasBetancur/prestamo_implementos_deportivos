package co.edu.itm.prestamosdeportivos.web.error;

import java.util.List;

import org.springframework.http.HttpStatus;

public class ExcepcionApi extends RuntimeException {
    private final HttpStatus estadoHttp;
    private final String codigo;
    private final String titulo;
    private final List<ErrorCampo> errores;

    public ExcepcionApi(HttpStatus estadoHttp, String codigo, String titulo, String detalle) {
        this(estadoHttp, codigo, titulo, detalle, List.of());
    }

    public ExcepcionApi(HttpStatus estadoHttp, String codigo, String titulo, String detalle,
            List<ErrorCampo> errores) {
        super(detalle);
        this.estadoHttp = estadoHttp;
        this.codigo = codigo;
        this.titulo = titulo;
        this.errores = List.copyOf(errores);
    }

    public HttpStatus getEstadoHttp() { return estadoHttp; }
    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public List<ErrorCampo> getErrores() { return errores; }
}
