package com.cloudkitchen.rbac.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application data initialization configuration properties
 * Binds app.data.* properties from application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "app.data")
public class AppDataProperties {

    private boolean initialize = false;

    // Getter and Setter
    public boolean isInitialize() {
        return initialize;
    }

    public void setInitialize(boolean initialize) {
        this.initialize = initialize;
    }
}
