package com.cloudkitchen.rbac.exception;

public class AuthExceptions {
    public static class UserAlreadyExistsException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public UserAlreadyExistsException(String message) { super(message); }
        public UserAlreadyExistsException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class UserNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public UserNotFoundException(String message) { super(message); }
    }
    
    public static class AccessDeniedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public AccessDeniedException(String message) { super(message); }
    }
    
    public static class TooManyAttemptsException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public TooManyAttemptsException(String message) { super(message); }
    }
    
    public static class InvalidPasswordException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public InvalidPasswordException(String message) { super(message); }
        public InvalidPasswordException(String message, Throwable cause) { super(message, cause); }
    }
}