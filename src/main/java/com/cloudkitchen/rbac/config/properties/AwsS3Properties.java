package com.cloudkitchen.rbac.config.properties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;


/**
 * AWS S3 configuration properties
 * Binds aws.s3.* properties from application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Validated
public class AwsS3Properties {

    @NotBlank(message = "AWS Access Key is required")
    private String accessKey;

    @NotBlank(message = "AWS Secret Key is required")
    private String secretKey;

    @NotBlank(message = "AWS Region is required")
    private String region;

    @NotBlank(message = "AWS S3 Bucket is required")
    private String bucket;

    private String endpoint = "";

    // Getters and Setters
    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
