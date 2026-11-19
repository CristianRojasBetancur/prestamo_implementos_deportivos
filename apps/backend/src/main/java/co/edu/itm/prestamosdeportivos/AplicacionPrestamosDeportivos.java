package co.edu.itm.prestamosdeportivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada de la API backend del Sistema de Préstamo de Implementos
 * Deportivos. El comportamiento funcional está gobernado por
 * {@code docs/specs/BE-01-api-backend-mvp.md}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AplicacionPrestamosDeportivos {

    public static void main(String[] args) {
        SpringApplication.run(AplicacionPrestamosDeportivos.class, args);
    }
}
