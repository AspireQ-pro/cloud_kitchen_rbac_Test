package com.cloudkitchen.rbac.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * Application upload configuration properties
 * Binds app.upload.* properties from application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "app.upload")
@Validated
public class AppUploadProperties {

    @Min(value = 1, message = "Max file size must be at least 1 byte")
    private long maxFileSize = 10485760; // 10MB default

    @NotEmpty(message = "Allowed types must not be empty")
    private List<String> allowedTypes = List.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    // Getters and Setters
    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }
}
