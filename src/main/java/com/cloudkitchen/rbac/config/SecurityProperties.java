package com.cloudkitchen.rbac.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Security configuration properties for environment-specific security settings.
 *
 * IMPORTANT: This class manages security-sensitive configurations.
 * - In DEVELOPMENT: OTP is stored in plaintext for easier testing (NOT SECURE)
 * - In PRODUCTION: OTP must be hashed before storage for security
 *
 * To enable production mode, set:
 * - SECURITY_ENVIRONMENT=prod
 * - HASH_OTP=true
 */
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private static final Logger log = LoggerFactory.getLogger(SecurityProperties.class);

    /**
     * Security environment: 'dev' or 'prod'
     * Default: 'dev'
     */
    private String environment = "dev";

    /**
     * Whether to hash OTP codes before storing in database
     * Default: false (development mode)
     * MUST be true in production
     */
    private boolean hashOtp = false;

    /**
     * Maximum login attempts before account lockout
     * Default: 5
     */
    private int maxLoginAttempts = 5;

    /**
     * Account lockout duration in minutes
     * Default: 30
     */
    private int lockoutDurationMinutes = 30;

    @PostConstruct
    public void init() {
        if ("dev".equalsIgnoreCase(environment) && !hashOtp) {
            log.warn("╔═══════════════════════════════════════════════════════════════════════╗");
            log.warn("║                    ⚠️  SECURITY WARNING  ⚠️                            ║");
            log.warn("╠═══════════════════════════════════════════════════════════════════════╣");
            log.warn("║  Application is running in DEVELOPMENT MODE with INSECURE settings:  ║");
            log.warn("║                                                                       ║");
            log.warn("║  • OTP codes stored in PLAINTEXT (NOT HASHED)                        ║");
            log.warn("║  • This is acceptable for DEVELOPMENT/TESTING only                   ║");
            log.warn("║  • DO NOT use these settings in PRODUCTION                           ║");
            log.warn("║                                                                       ║");
            log.warn("║  To enable production mode, set:                                     ║");
            log.warn("║    SECURITY_ENVIRONMENT=prod                                          ║");
            log.warn("║    HASH_OTP=true                                                      ║");
            log.warn("╚═══════════════════════════════════════════════════════════════════════╝");
        } else if ("prod".equalsIgnoreCase(environment)) {
            if (!hashOtp) {
                throw new IllegalStateException(
                    "CRITICAL SECURITY ERROR: Running in production mode but OTP hashing is DISABLED. " +
                    "Set HASH_OTP=true in production environment."
                );
            }
            log.info("✅ Security configuration: PRODUCTION mode with OTP hashing ENABLED");
        }

        log.info("Security configuration loaded: environment={}, hashOtp={}, maxLoginAttempts={}",
                environment, hashOtp, maxLoginAttempts);
    }

    // Getters and Setters

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isHashOtp() {
        return hashOtp;
    }

    public void setHashOtp(boolean hashOtp) {
        this.hashOtp = hashOtp;
    }

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    /**
     * Check if application is running in development mode
     */
    public boolean isDevelopmentMode() {
        return "dev".equalsIgnoreCase(environment);
    }

    /**
     * Check if application is running in production mode
     */
    public boolean isProductionMode() {
        return "prod".equalsIgnoreCase(environment);
    }
}
