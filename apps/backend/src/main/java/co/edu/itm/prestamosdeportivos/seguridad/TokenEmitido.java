package co.edu.itm.prestamosdeportivos.seguridad;

import java.time.Instant;

/** JWT firmado y su instante de expiración. */
public record TokenEmitido(String valor, Instant expiraEn) {

    @Override
    public String toString() {
        return "TokenEmitido[valor=***, expiraEn=" + expiraEn + "]";
    }
}
