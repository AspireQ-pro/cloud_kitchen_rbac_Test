package com.cloudkitchen.rbac.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "aws.s3")
@Validated
public class S3Properties {
    
    @NotBlank(message = "AWS S3 access key is required")
    private String accessKey;
    
    @NotBlank(message = "AWS S3 secret key is required")
    private String secretKey;
    
    @NotBlank(message = "AWS S3 region is required")
    private String region;
    
    @NotBlank(message = "AWS S3 bucket name is required")
    private String bucket;
    
    private String endpoint;
    
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
