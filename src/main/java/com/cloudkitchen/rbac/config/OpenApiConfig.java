package com.cloudkitchen.rbac.config;

import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration for Cloud Kitchen RBAC Service
 *
 * The OpenAPI specification is defined in YAML files located at:
 * src/main/resources/openapi/
 *
 * Main specification: openapi.yaml (references all domain-specific YAML files)
 *
 * Access Swagger UI at: http://localhost:8081/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8081/v3/api-docs
 *
 * Configuration is managed via application.properties:
 * - springdoc.api-docs.enabled=true
 * - springdoc.swagger-ui.enabled=true
 */
@Configuration
public class OpenApiConfig {
    // Configuration is now fully managed by YAML files in src/main/resources/openapi/
    // No programmatic configuration needed - SpringDoc will auto-discover the YAML files
}
