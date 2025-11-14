package com.cloudkitchen.rbac.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;



    @Bean
    @Primary
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
             .region(Region.of(region))
             .credentialsProvider(StaticCredentialsProvider.create(credentials))
             .overrideConfiguration(ClientOverrideConfiguration.builder()
                 .retryPolicy(RetryPolicy.builder()
                     .numRetries(3)
                     .build())
                 .apiCallTimeout(Duration.ofMinutes(2))
                 .apiCallAttemptTimeout(Duration.ofSeconds(30))
                 .build())
             .build();
     }

     @Bean
     @Primary
     public S3AsyncClient s3AsyncClient() {
         AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

         return S3AsyncClient.builder()
             .region(Region.of(region))
             .credentialsProvider(StaticCredentialsProvider.create(credentials))
             .overrideConfiguration(ClientOverrideConfiguration.builder()
                 .retryPolicy(RetryPolicy.builder()
                     .numRetries(3)
                     .build())
                 .apiCallTimeout(Duration.ofMinutes(2))
                 .apiCallAttemptTimeout(Duration.ofSeconds(30))
                 .build())
             .build();
     }

     @Bean
     @Primary
     public S3Presigner s3Presigner() {
         AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

         return S3Presigner.builder()
             .region(Region.of(region))
             .credentialsProvider(StaticCredentialsProvider.create(credentials))
             .build();
     }
}