package com.cloudkitchen.rbac.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for Cloud Kitchen RBAC Service
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Value("${app.api.base-url:}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        String localUrl = "http://localhost:" + serverPort;
        
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud Kitchen RBAC Service API")
                        .description("Role-Based Access Control API for Cloud Kitchen platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cloud Kitchen RBAC Service")
                                .email("support@cloudkitchen.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://cloudkitchen.com/license")))
                .servers(baseUrl.isEmpty() ? 
                    List.of(new Server().url(localUrl).description("Development server")) :
                    List.of(
                        new Server().url(localUrl).description("Development server"),
                        new Server().url(baseUrl).description("Production server")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
