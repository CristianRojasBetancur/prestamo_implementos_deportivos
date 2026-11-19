package co.edu.itm.prestamosdeportivos.aplicacion.sanciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import co.edu.itm.prestamosdeportivos.dominio.AlcanceDevolucion;
import co.edu.itm.prestamosdeportivos.dominio.Devolucion;
import co.edu.itm.prestamosdeportivos.dominio.Prestamo;
import co.edu.itm.prestamosdeportivos.dominio.Usuario;
import org.junit.jupiter.api.Test;

class PoliticasSancionTest {

    private static final Instant LIMITE = Instant.parse("2026-11-19T15:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-11-20T18:00:00Z");

    private final SancionPorRetrasoSinJustificacion sinJustificacion = new SancionPorRetrasoSinJustificacion();
    private final SinSancionPorRetrasoJustificado justificada = new SinSancionPorRetrasoJustificado();

    @Test
    void retrasoSinJustificacionDuplicaRetraso() {
        var resultado = sinJustificacion.calcular(contexto(AHORA, false)).orElseThrow();

        assertThat(resultado.minutosRetraso()).isEqualTo(Duration.between(LIMITE, AHORA).toMinutes());
        assertThat(resultado.duracion()).isEqualTo(Duration.ofDays(2).plusHours(6));
    }

    @Test
    void retrasoMenorAUnDiaRespetaMinimoDeDosDias() {
        Instant ahora = LIMITE.plus(Duration.ofMinutes(30));

        var resultado = sinJustificacion.calcular(contexto(ahora, false)).orElseThrow();

        assertThat(resultado.minutosRetraso()).isEqualTo(30);
        assertThat(resultado.duracion()).isEqualTo(Duration.ofDays(2));
    }

    @Test
    void dentroDelPlazoNoAplicaSancion() {
        Instant ahora = LIMITE.minusSeconds(1);

        assertThat(sinJustificacion.aplica(contexto(ahora, false))).isFalse();
    }

    @Test
    void retrasoJustificadoNoGeneraSancion() {
        var contexto = contexto(AHORA, true);

        assertThat(justificada.aplica(contexto)).isTrue();
        assertThat(justificada.calcular(contexto)).isEmpty();
        assertThat(sinJustificacion.aplica(contexto)).isFalse();
    }

    private ContextoSancion contexto(Instant ahora, boolean retrasoJustificado) {
        Usuario usuario = Usuario.solicitante("123", "Ana", "Rojas");
        Usuario prestador = Usuario.conAcceso("456", "Pedro", "Perez", "p@itm.edu.co", "hash",
                co.edu.itm.prestamosdeportivos.dominio.Rol.PRESTADOR);
        Prestamo prestamo = new Prestamo(usuario, prestador, LIMITE.minus(Duration.ofDays(1)), LIMITE);
        Devolucion devolucion = new Devolucion(prestamo, prestador, ahora, AlcanceDevolucion.COMPLETA,
                retrasoJustificado, retrasoJustificado ? "Fuerza mayor" : null, null);
        return new ContextoSancion(prestamo, devolucion, ahora);
    }
}
