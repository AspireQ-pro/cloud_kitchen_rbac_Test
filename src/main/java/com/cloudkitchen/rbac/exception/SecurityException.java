package com.cloudkitchen.rbac.exception;

/**
 * Custom security exception for RBAC system
 */
public class SecurityException extends RuntimeException {
    
    private final String errorCode;
    private final String userMessage;
    
    public SecurityException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public SecurityException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public String getErrorCode() { return errorCode; }
    public String getUserMessage() { return userMessage; }
    
    // Common security exceptions
    public static SecurityException invalidInput(String field) {
        return new SecurityException("SEC001", 
            "Invalid input detected in field: " + field, 
            "Invalid characters detected in input");
    }
    
    public static SecurityException xssAttempt(String field) {
        return new SecurityException("SEC002", 
            "XSS attempt detected in field: " + field, 
            "Invalid characters detected");
    }
    
    public static SecurityException sqlInjectionAttempt(String field) {
        return new SecurityException("SEC003", 
            "SQL injection attempt detected in field: " + field, 
            "Invalid characters detected");
    }
    
    public static SecurityException rateLimitExceeded() {
        return new SecurityException("SEC004", 
            "Rate limit exceeded", 
            "Too many requests. Please try again later");
    }
}