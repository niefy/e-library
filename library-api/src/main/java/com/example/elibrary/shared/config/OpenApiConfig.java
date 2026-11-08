package com.example.elibrary.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eLibraryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Library Service API")
                        .version("1.0.0")
                        .description("""
                                Catalog browsing, borrowing and returning.
                                Local development uses a dev-only HMAC token endpoint; production is
                                expected to use an OIDC provider.
                                """)
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
