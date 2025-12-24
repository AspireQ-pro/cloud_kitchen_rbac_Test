package com.cloudkitchen.rbac.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration to serve OpenAPI YAML files as static resources
 *
 * This allows Swagger UI to access the YAML files at /openapi/**
 */
@Configuration
public class OpenApiResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve OpenAPI YAML files from classpath
        registry.addResourceHandler("/openapi/**")
                .addResourceLocations("classpath:/openapi/")
                .setCachePeriod(0); // Disable caching for development
    }
}
