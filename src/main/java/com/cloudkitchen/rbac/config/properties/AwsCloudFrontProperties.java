package com.cloudkitchen.rbac.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AWS CloudFront configuration properties
 * Binds aws.cloudfront.* properties from application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "aws.cloudfront")
public class AwsCloudFrontProperties {

    private boolean enabled = false;
    private String url = "";
    private String keyPairId = "";
    private String privateKeyPath = "";

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKeyPairId() {
        return keyPairId;
    }

    public void setKeyPairId(String keyPairId) {
        this.keyPairId = keyPairId;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }
}
