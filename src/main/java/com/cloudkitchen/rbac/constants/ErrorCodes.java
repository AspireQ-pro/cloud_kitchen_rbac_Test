package com.cloudkitchen.rbac.constants;

public class ErrorCodes {
    
    // Validation Errors (1000-1099)
    public static final String VALIDATION_FAILED = "ERR_1001";
    public static final String INVALID_PHONE_FORMAT = "ERR_1002";
    public static final String INVALID_PASSWORD_FORMAT = "ERR_1003";
    public static final String INVALID_OTP_FORMAT = "ERR_1004";
    public static final String MISSING_REQUIRED_FIELD = "ERR_1005";
    public static final String INVALID_MERCHANT_ID = "ERR_1006";
    public static final String INVALID_NAME_FORMAT = "ERR_1007";
    public static final String INVALID_EMAIL_FORMAT = "ERR_1008";
    
    // Authentication Errors (2000-2099)
    public static final String INVALID_CREDENTIALS = "ERR_2001";
    public static final String USER_NOT_FOUND = "ERR_2002";
    public static final String INVALID_TOKEN = "ERR_2003";
    public static final String TOKEN_EXPIRED = "ERR_2004";
    public static final String TOKEN_BLACKLISTED = "ERR_2005";
    public static final String AUTHENTICATION_FAILED = "ERR_2006";
    public static final String INVALID_REFRESH_TOKEN = "ERR_2007";
    
    // Authorization Errors (3000-3099)
    public static final String ACCESS_DENIED = "ERR_3001";
    public static final String INSUFFICIENT_PERMISSIONS = "ERR_3002";
    public static final String ROLE_NOT_FOUND = "ERR_3003";
    public static final String MERCHANT_ACCESS_DENIED = "ERR_3004";
    
    // User Management Errors (4000-4099)
    public static final String USER_ALREADY_EXISTS = "ERR_4001";
    public static final String USER_CREATION_FAILED = "ERR_4002";
    public static final String USER_UPDATE_FAILED = "ERR_4003";
    public static final String USER_DELETION_FAILED = "ERR_4004";
    public static final String PHONE_ALREADY_REGISTERED = "ERR_4005";
    public static final String EMAIL_ALREADY_REGISTERED = "ERR_4006";
    
    // OTP Errors (5000-5099)
    public static final String OTP_INVALID = "ERR_5001";
    public static final String OTP_EXPIRED = "ERR_5002";
    public static final String OTP_SEND_FAILED = "ERR_5003";
    public static final String OTP_MAX_ATTEMPTS = "ERR_5004";
    public static final String OTP_NOT_FOUND = "ERR_5005";
    public static final String OTP_ALREADY_VERIFIED = "ERR_5006";
    
    // Session Errors (6000-6099)
    public static final String SESSION_EXPIRED = "ERR_6001";
    public static final String SESSION_INVALID = "ERR_6002";
    public static final String MAX_SESSIONS_EXCEEDED = "ERR_6003";
    public static final String SESSION_CREATION_FAILED = "ERR_6004";
    
    // Merchant Errors (7000-7099)
    public static final String MERCHANT_NOT_FOUND = "ERR_7001";
    public static final String MERCHANT_INACTIVE = "ERR_7002";
    public static final String MERCHANT_ACCESS_RESTRICTED = "ERR_7003";
    
    // System Errors (9000-9099)
    public static final String INTERNAL_SERVER_ERROR = "ERR_9001";
    public static final String DATABASE_ERROR = "ERR_9002";
    public static final String EXTERNAL_SERVICE_ERROR = "ERR_9003";
    public static final String CONFIGURATION_ERROR = "ERR_9004";
    public static final String RATE_LIMIT_EXCEEDED = "ERR_9005";
    public static final String SERVICE_UNAVAILABLE = "ERR_9006";
    public static final String BAD_GATEWAY = "ERR_9007";
    public static final String GATEWAY_TIMEOUT = "ERR_9008";
    
    // Resource Errors (8000-8099)
    public static final String RESOURCE_NOT_FOUND = "ERR_8001";
    public static final String RESOURCE_CONFLICT = "ERR_8002";
    public static final String UNPROCESSABLE_ENTITY = "ERR_8003";
    public static final String METHOD_NOT_ALLOWED = "ERR_8004";
    public static final String UNSUPPORTED_MEDIA_TYPE = "ERR_8005";
}