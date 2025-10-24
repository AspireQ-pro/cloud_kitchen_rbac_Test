package com.cloudkitchen.rbac.exception;

/**
 * Custom business exceptions for better error handling and API responses
 */
public class BusinessExceptions {
    
    public static class UserNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public UserNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class MerchantNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public MerchantNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class MerchantAlreadyExistsException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public MerchantAlreadyExistsException(String message) {
            super(message);
        }
    }
    
    public static class UserAlreadyExistsException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }
    
    public static class InvalidCredentialsException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
    
    public static class AccessDeniedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public AccessDeniedException(String message) {
            super(message);
        }
    }
    
    public static class OtpException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public OtpException(String message) {
            super(message);
        }
    }
    
    public static class RateLimitExceededException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
    
    public static class ValidationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public ValidationException(String message) {
            super(message);
        }
    }
    
    public static class TokenExpiredException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public TokenExpiredException(String message) {
            super(message);
        }
    }
    
    public static class ServiceUnavailableException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public ServiceUnavailableException(String message) {
            super(message);
        }
    }
    
    public static class FileUploadException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public FileUploadException(String message) {
            super(message);
        }
    }
}