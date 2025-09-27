package com.cloudkitchen.rbac.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Session Management Configuration
 * 
 * Token Expiry Times:
 * - Access Token: 24 hours (86400 seconds)
 * - Refresh Token: 7 days (604800 seconds)  
 * - OTP: 10 minutes (600 seconds)
 * - User Session: 24 hours (matches access token)
 * 
 * Session Features:
 * - Automatic cleanup of expired sessions (hourly)
 * - Session tracking with token hash
 * - Multi-device session support
 * - Concurrent session limits (configurable)
 * - Session invalidation on logout
 */
@Configuration
@EnableScheduling
public class SessionConfig {
    
    // Session expiry constants
    public static final int ACCESS_TOKEN_EXPIRY_SECONDS = 86400; // 24 hours
    public static final int REFRESH_TOKEN_EXPIRY_SECONDS = 604800; // 7 days
    public static final int OTP_EXPIRY_MINUTES = 10; // 10 minutes
    public static final int SESSION_CLEANUP_INTERVAL_MS = 3600000; // 1 hour
    
    // Session limits
    public static final int MAX_CONCURRENT_SESSIONS = 5; // Per user
}