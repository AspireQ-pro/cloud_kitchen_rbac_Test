package com.cloudkitchen.rbac.constants;

public final class SecurityConstants {
    
    // JWT Constants
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";
    public static final String JWT_ISSUER = "cloud-kitchen-rbac";
    public static final int JWT_ACCESS_EXPIRY_SECONDS = 3600; // 1 hour
    public static final int JWT_REFRESH_EXPIRY_SECONDS = 604800; // 7 days
    
    // OTP Constants
    public static final int OTP_LENGTH = 4;
    public static final int OTP_EXPIRY_MINUTES = 5;
    public static final int MAX_OTP_ATTEMPTS = 3;
    public static final int OTP_BLOCK_MINUTES = 15;
    
    // Rate Limiting
    public static final int DEFAULT_RATE_LIMIT = 60;
    public static final int AUTH_RATE_LIMIT = 10;
    
    // Password Policy
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    
    // User Types
    public static final String USER_TYPE_SUPER_ADMIN = "super_admin";
    public static final String USER_TYPE_MERCHANT = "merchant";
    public static final String USER_TYPE_CUSTOMER = "customer";
    
    // Roles
    public static final String ROLE_SUPER_ADMIN = "super_admin";
    public static final String ROLE_MERCHANT_ADMIN = "merchant_admin";
    public static final String ROLE_MERCHANT_MANAGER = "merchant_manager";
    public static final String ROLE_MERCHANT_STAFF = "merchant_staff";
    public static final String ROLE_CUSTOMER = "customer";
    
    private SecurityConstants() {}
}