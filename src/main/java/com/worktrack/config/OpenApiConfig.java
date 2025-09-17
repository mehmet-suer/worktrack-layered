package com.worktrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI worktrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WorkTrack API")
                        .description("API documentation for WorkTrack application")
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(SecurityConstants.BEARER_AUTH_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme(SecurityConstants.SCHEME_BEARER)
                                        .bearerFormat(SecurityConstants.BEARER_FORMAT_JWT)
                        ))
                .addSecurityItem(new SecurityRequirement().addList(SecurityConstants.BEARER_AUTH_SCHEME));
    }
}
