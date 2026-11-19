package co.edu.itm.prestamosdeportivos.configuracion;

import java.util.Map;

import co.edu.itm.prestamosdeportivos.web.error.ProblemaRespuesta;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documento OpenAPI alineado con BE-01: esquema de seguridad Bearer (JWT),
 * formato de error común y respuestas de error estándar en cada operación.
 * Disponible en {@code /v3/api-docs} y {@code /swagger-ui.html}.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionOpenApi {

    public static final String ESQUEMA_SEGURIDAD = "bearerAuth";
    private static final String TIPO_PROBLEMA = "application/problem+json";
    private static final String REF_PROBLEMA = "#/components/schemas/ProblemaRespuesta";

    private static final Map<String, String> ERRORES_ESTANDAR = Map.of(
            "400", "Solicitud mal formada o datos inválidos (SOLICITUD_INVALIDA, VALIDACION_FALLIDA).",
            "401", "Sin autenticación o sesión inválida (NO_AUTENTICADO, SESION_INVALIDA).",
            "403", "El rol no permite la operación (ACCESO_DENEGADO).",
            "404", "Recurso inexistente (RECURSO_NO_ENCONTRADO).",
            "409", "Conflicto con el estado actual, duplicidad o concurrencia.",
            "422", "Regla de negocio no cumplida.",
            "500", "Error interno (ERROR_INTERNO); incluye correlacionId.");

    @Bean
    OpenAPI documentoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Sistema de Préstamo de Implementos Deportivos")
                        .version("v1")
                        .description("Contrato de la API del MVP. Especificación: docs/specs/BE-01-api-backend-mvp.md; "
                                + "convenciones y errores: docs/specs/BE-01-A-contratos-y-errores.md. "
                                + "Todas las rutas usan el prefijo /api/v1; las fechas son ISO-8601 con zona horaria "
                                + "y los identificadores son UUID."))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token emitido por POST /api/v1/auth/login. Cabecera: Authorization: Bearer <token>.")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD));
    }

    /** Registra el esquema de error y lo agrega como respuesta en cada operación documentada. */
    @Bean
    OpenApiCustomizer respuestasDeErrorComunes() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            @SuppressWarnings("rawtypes")
            Map<String, Schema> esquemas = ModelConverters.getInstance().readAll(ProblemaRespuesta.class);
            esquemas.forEach(openApi.getComponents()::addSchemas);

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(ruta -> ruta.readOperations().forEach(this::agregarErrores));
        };
    }

    private void agregarErrores(Operation operacion) {
        if (operacion.getResponses() == null) {
            operacion.setResponses(new ApiResponses());
        }
        ERRORES_ESTANDAR.forEach((codigo, descripcion) -> {
            if (!operacion.getResponses().containsKey(codigo)) {
                operacion.getResponses().addApiResponse(codigo, new ApiResponse()
                        .description(descripcion)
                        .content(new Content().addMediaType(TIPO_PROBLEMA,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(new Schema<>().$ref(REF_PROBLEMA)))));
            }
        });
    }
}
