package com.cloudkitchen.rbac.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.cloudkitchen.rbac.util.ResponseBuilder;
import com.cloudkitchen.rbac.util.ValidationUtils;
import com.cloudkitchen.rbac.exception.BusinessExceptions.*;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        
        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getAllErrors().stream()
            .map(err -> {
                FieldError fieldError = (FieldError) err;
                Map<String, Object> error = new HashMap<>();
                error.put("field", fieldError.getField());
                error.put("message", fieldError.getDefaultMessage());
                error.put("rejectedValue", sanitizeRejectedValue(fieldError.getRejectedValue()));
                return error;
            })
            .collect(Collectors.toList());
        
        // Return the first validation error message directly
        String errorMessage = fieldErrors.isEmpty() ? "Validation failed" : 
            fieldErrors.get(0).get("message").toString();
            
        Map<String, Object> response = ResponseBuilder.error(400, errorMessage);
        response.put("fieldErrors", fieldErrors);
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Validation failed with {} errors", fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "Constraint validation failed");
        
        List<Map<String, Object>> fieldErrors = ex.getConstraintViolations().stream()
            .map(violation -> {
                Map<String, Object> error = new HashMap<>();
                error.put("field", violation.getPropertyPath().toString());
                error.put("message", violation.getMessage());
                error.put("rejectedValue", sanitizeRejectedValue(violation.getInvalidValue()));
                return error;
            })
            .collect(Collectors.toList());
        
        response.put("fieldErrors", fieldErrors);
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Constraint violation with {} errors", fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, "User not found");
        response.put("details", sanitizeErrorMessage(ex.getMessage()));
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("User not found: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMerchantNotFoundException(MerchantNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, "Merchant not found");
        response.put("details", sanitizeErrorMessage(ex.getMessage()));
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Merchant not found: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler({MerchantAlreadyExistsException.class, UserAlreadyExistsException.class})
    public ResponseEntity<Map<String, Object>> handleAlreadyExistsException(RuntimeException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(409, "Resource already exists");
        response.put("details", sanitizeErrorMessage(ex.getMessage()));
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Resource conflict: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    @ExceptionHandler({InvalidCredentialsException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleSecurityException(RuntimeException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(401, "Authentication failed");
        response.put("details", "Invalid credentials or access denied");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Security exception: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitExceededException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(429, "Rate limit exceeded");
        response.put("details", sanitizeErrorMessage(ex.getMessage()));
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Rate limit exceeded: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "Invalid argument");
        response.put("details", sanitizeErrorMessage(ex.getMessage()));
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Invalid argument: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(500, "Runtime error");
        response.put("details", sanitizeErrorMessage(ex.getMessage()));
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Runtime exception: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "Invalid request format");
        
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("JSON parse error")) {
                response.put("details", "Invalid JSON format in request body");
            } else if (message.contains("Required request body is missing")) {
                response.put("details", "Request body is required");
            } else {
                response.put("details", "The request body contains invalid JSON or missing required fields");
            }
        } else {
            response.put("details", "Invalid request format");
        }
        
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Invalid request format: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(500, "Internal server error");
        response.put("details", "An unexpected error occurred while processing your request");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("Unexpected exception in {}: {}", request.getDescription(false), sanitizeLogMessage(ex.getMessage()), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) return "Invalid request";
        return message.replaceAll("[\\r\\n\\t]", "").trim();
    }
    
    private String sanitizeLogMessage(String message) {
        if (message == null) return "[NULL]";
        // Prevent log injection by removing CRLF and control characters
        String sanitized = message.replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F<>\"'&;%${}\\[\\]()]", "_")
                                 .replaceAll("(?i)(script|javascript|vbscript|onload|onerror)", "[FILTERED]")
                                 .trim();
        return sanitized.length() > 200 ? sanitized.substring(0, 200) + "..." : sanitized;
    }
    
    private Object sanitizeRejectedValue(Object value) {
        if (value == null) return null;
        String strValue = value.toString();
        // Mask sensitive fields like passwords, tokens, OTPs
        if (strValue.length() > 50 || 
            strValue.matches(".*(?i)(password|token|otp|secret|key).*")) {
            return "[MASKED]"; 
        }
        // Mask phone numbers
        if (ValidationUtils.isValidPhone(strValue)) {
            return "****" + strValue.substring(strValue.length() - 4);
        }
        return value;
    }
}