package co.edu.itm.prestamosdeportivos.soporte;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import co.edu.itm.prestamosdeportivos.persistencia.CategoriaRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.ImplementoDeportivoRepositorio;
import co.edu.itm.prestamosdeportivos.persistencia.UsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Beans comunes a las suites E2E; no se carga fuera de {@code src/test}. */
@TestConfiguration(proxyBeanMethods = false)
public class FixturesConfiguracion {

    public static final Instant AHORA = Instant.parse("2026-09-01T15:00:00Z");

    @Bean
    @Primary
    RelojPruebas relojDePrueba() {
        return new RelojPruebas(AHORA, ZoneId.of("America/Bogota"));
    }

    @Bean
    FabricaFixtures fabricaFixtures(UsuarioRepositorio usuarios, CategoriaRepositorio categorias,
            ImplementoDeportivoRepositorio implementos, PasswordEncoder contrasenas, Clock relojDePrueba) {
        return new FabricaFixtures(usuarios, categorias, implementos, contrasenas, relojDePrueba);
    }

    /** Reloj mutable únicamente disponible en el contexto de pruebas. */
    public static final class RelojPruebas extends Clock {
        private Instant actual;
        private final ZoneId zona;

        public RelojPruebas(Instant inicial, ZoneId zona) {
            this.actual = inicial;
            this.zona = zona;
        }

        @Override
        public synchronized Instant instant() {
            return actual;
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public synchronized Clock withZone(ZoneId nuevaZona) {
            return new RelojPruebas(actual, nuevaZona);
        }

        public synchronized void fijar(Instant nuevoInstante) {
            actual = nuevoInstante;
        }
    }
}
