package com.cloudkitchen.rbac.config;

import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {
    
    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);
    
    private final S3Properties properties;
    
    public S3Config(S3Properties properties) {
        this.properties = properties;
    }
    
    @Bean
    public S3AsyncClient s3AsyncClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            properties.getAccessKey(),
            properties.getSecretKey()
        );
        
        software.amazon.awssdk.services.s3.S3AsyncClientBuilder builder = software.amazon.awssdk.services.s3.S3AsyncClient.builder()
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials));
        
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        
        return builder.build();
    }
    
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            properties.getAccessKey(),
            properties.getSecretKey()
        );
        
        software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder builder = software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials));
        
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        
        return builder.build();
    }
    
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            properties.getAccessKey(),
            properties.getSecretKey()
        );
        
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(properties.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder().numRetries(3).build())
                .apiCallTimeout(Duration.ofMinutes(2))
                .apiCallAttemptTimeout(Duration.ofSeconds(30))
                .build());
        
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
            logger.info("S3 endpoint override: {}", properties.getEndpoint());
        }
        
        S3Client client = builder.build();
        logger.info("S3Client initialized for region: {}", properties.getRegion());
        
        validateS3Configuration(client);
        
        return client;
    }
    
    private void validateS3Configuration(S3Client s3Client) {
        logger.info("Validating S3 configuration...");
        
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(properties.getBucket())
                .build();
            
            s3Client.headBucket(headBucketRequest);
            
            validatePermissions(s3Client);
            
            logger.info("✅ S3 validation successful - Bucket '{}' accessible in region '{}'",
                properties.getBucket(), properties.getRegion());
            
        } catch (NoSuchBucketException e) {
            String error = String.format(
                "❌ S3 bucket '%s' does not exist in region '%s'",
                properties.getBucket(), properties.getRegion()
            );
            logger.error(error);
            throw new IllegalStateException(error, e);
            
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                String error = String.format(
                    "❌ Access denied to S3 bucket '%s' - verify IAM permissions",
                    properties.getBucket()
                );
                logger.error(error);
                throw new IllegalStateException(error, e);
            } else if (e.statusCode() == 301) {
                String error = String.format(
                    "❌ Bucket '%s' exists in different region - current: '%s'",
                    properties.getBucket(), properties.getRegion()
                );
                logger.error(error);
                throw new IllegalStateException(error, e);
            }
            throw new IllegalStateException("❌ S3 validation failed: " + e.awsErrorDetails().errorMessage(), e);
            
        } catch (Exception e) {
            throw new IllegalStateException("❌ S3 validation failed: " + e.getMessage(), e);
        }
    }
    
    private void validatePermissions(S3Client s3Client) {
        try {
            String testKey = ".validation-test";
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(testKey)
                .build();
            
            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromString("test"));
            
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(testKey)
                .build());
            
            logger.info("✅ S3 permissions validated (PutObject, DeleteObject)");
            
        } catch (S3Exception e) {
            throw new IllegalStateException(
                "❌ Missing S3 permissions - ensure PutObject and DeleteObject are granted", e
            );
        }
    }
}
