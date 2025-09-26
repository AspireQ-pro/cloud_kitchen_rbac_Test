package com.cloudkitchen.rbac.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthExceptions.UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(AuthExceptions.UserAlreadyExistsException ex) {
        return buildErrorResponse(409, ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthExceptions.UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(AuthExceptions.UserNotFoundException ex) {
        return buildErrorResponse(404, ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthExceptions.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AuthExceptions.AccessDeniedException ex) {
        return buildErrorResponse(403, ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthExceptions.TooManyAttemptsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyAttempts(AuthExceptions.TooManyAttemptsException ex) {
        return buildErrorResponse(429, ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(AuthExceptions.InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPassword(AuthExceptions.InvalidPasswordException ex) {
        logger.warn("Invalid password attempt: {}", ex.getMessage());
        return buildErrorResponse(401, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("IllegalArgumentException: {}", ex.getMessage());
        return buildErrorResponse(400, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.error("Data integrity violation: {}", ex.getMessage());
        String message = "Data conflict occurred";
        if (ex.getMessage() != null && ex.getMessage().contains("uk_users_merchant_phone")) {
            message = "Phone number already registered with this merchant";
        }
        return buildErrorResponse(409, message, HttpStatus.CONFLICT);
    }


    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.append(fieldName).append(": ").append(errorMessage).append("; ");
            } else {
                errors.append(error.getDefaultMessage()).append("; ");
            }
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 400);
        response.put("message", "Validation failed: " + errors.toString());
        response.put("success", false);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected exception: {}", ex.getMessage(), ex);
        String detailedMessage = ex.getMessage() != null ? ex.getMessage() : "Unexpected error: " + ex.getClass().getSimpleName();
        return buildErrorResponse(500, detailedMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(int code, String message, HttpStatus status) {
        return ResponseEntity.status(status).body(buildErrorMap(code, message));
    }

    private Map<String, Object> buildErrorMap(int code, String message) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("success", false);
        return response;
    }
}