package com.cloudkitchen.rbac.constants;

public final class ErrorCodes {
    
    // Authentication Errors
    public static final String INVALID_CREDENTIALS = "AUTH_001";
    public static final String USER_NOT_FOUND = "AUTH_002";
    public static final String USER_ALREADY_EXISTS = "AUTH_003";
    public static final String INVALID_TOKEN = "AUTH_004";
    public static final String TOKEN_EXPIRED = "AUTH_005";
    public static final String ACCESS_DENIED = "AUTH_006";
    public static final String ACCOUNT_LOCKED = "AUTH_007";
    public static final String INVALID_OTP = "AUTH_008";
    public static final String OTP_EXPIRED = "AUTH_009";
    public static final String TOO_MANY_ATTEMPTS = "AUTH_010";
    
    // Validation Errors
    public static final String VALIDATION_FAILED = "VAL_001";
    public static final String INVALID_PHONE = "VAL_002";
    public static final String INVALID_EMAIL = "VAL_003";
    public static final String INVALID_PASSWORD = "VAL_004";
    public static final String REQUIRED_FIELD_MISSING = "VAL_005";
    public static final String INVALID_FORMAT = "VAL_006";
    
    // Business Logic Errors
    public static final String MERCHANT_NOT_FOUND = "BIZ_001";
    public static final String ROLE_NOT_FOUND = "BIZ_002";
    public static final String PERMISSION_DENIED = "BIZ_003";
    public static final String INVALID_OPERATION = "BIZ_004";
    public static final String RESOURCE_NOT_FOUND = "BIZ_005";
    
    // System Errors
    public static final String INTERNAL_ERROR = "SYS_001";
    public static final String DATABASE_ERROR = "SYS_002";
    public static final String EXTERNAL_SERVICE_ERROR = "SYS_003";
    public static final String RATE_LIMIT_EXCEEDED = "SYS_004";
    
    private ErrorCodes() {}
}