package com.cloudkitchen.rbac.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Configuration
public class PrometheusMetricsConfig {

    @Bean
    public Counter authLoginAttempts(MeterRegistry registry) {
        return Counter.builder("auth_login_attempts_total")
                .description("Total login attempts")
                .register(registry);
    }

    @Bean
    public Counter authLoginSuccess(MeterRegistry registry) {
        return Counter.builder("auth_login_success_total")
                .description("Successful login attempts")
                .register(registry);
    }

    @Bean
    public Counter authLoginFailures(MeterRegistry registry) {
        return Counter.builder("auth_login_failures_total")
                .description("Failed login attempts")
                .register(registry);
    }

    @Bean
    public Counter otpRequests(MeterRegistry registry) {
        return Counter.builder("otp_requests_total")
                .description("Total OTP requests")
                .register(registry);
    }

    @Bean
    public Counter s3Uploads(MeterRegistry registry) {
        return Counter.builder("s3_uploads_total")
                .description("Total S3 file uploads")
                .register(registry);
    }

    @Bean
    public Timer s3UploadDuration(MeterRegistry registry) {
        return Timer.builder("s3_upload_duration_seconds")
                .description("S3 upload duration")
                .register(registry);
    }

    @Bean
    public Timer databaseQueryDuration(MeterRegistry registry) {
        return Timer.builder("database_query_duration_seconds")
                .description("Database query execution time")
                .register(registry);
    }
}