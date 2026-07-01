package com.gdl.facturacion_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger / OpenAPI. Define el botón "Authorize" con JWT Bearer
 * para poder probar los endpoints protegidos desde la UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI portalMotoresOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Facturacion DTE")
                        .version("1.0")
                        .description("API Backend (Spring Boot) - Facturación electrónica. "
                                + "Usa Authorize con el token JWT obtenido en /auth/login."))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
