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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.cloudkitchen.rbac.util.ResponseBuilder;
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
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        String traceId = response.get("traceId").toString();
        logger.warn("Validation failed [{}] with {} field errors", traceId, fieldErrors.size());
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
        
        String traceId = response.get("traceId").toString();
        logger.warn("Constraint violation [{}] with {} errors", traceId, fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, "User not found");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "The requested user does not exist");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("User not found for request");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMerchantNotFoundException(MerchantNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, "Merchant not found");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "The requested merchant does not exist");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Merchant not found for request");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler({MerchantAlreadyExistsException.class, UserAlreadyExistsException.class})
    public ResponseEntity<Map<String, Object>> handleAlreadyExistsException(RuntimeException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(409, sanitizeErrorMessage(ex.getMessage()));
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Resource conflict occurred: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String message = ex.getMessage();
        String userFriendlyMessage = getUserFriendlyConstraintMessage(message);
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> response = ResponseBuilder.error(409, userFriendlyMessage);
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", traceId);
        
        logger.warn("Data integrity violation [{}]: {}", traceId, sanitizeLogMessage(message));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    private String getUserFriendlyConstraintMessage(String message) {
        if (message == null) return "Data constraint violation";
        
        if (message.contains("merchants_email_key")) {
            return "A merchant with this email address already exists. Please use a different email.";
        } else if (message.contains("merchants_phone_key")) {
            return "A merchant with this phone number already exists. Please use a different phone number.";
        } else if (message.contains("users_username_key")) {
            return "This username is already taken. Please choose a different username.";
        } else if (message.contains("users_phone_key")) {
            return "A user with this phone number already exists. Please use a different phone number.";
        } else if (message.contains("users_email_key")) {
            return "A user with this email address already exists. Please use a different email.";
        } else if (message.contains("duplicate key")) {
            return "This information is already registered in the system. Please check your details.";
        }
        return "Data constraint violation";
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentialsException(InvalidCredentialsException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(401, "Authentication failed");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "Invalid credentials provided");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Invalid credentials provided");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(BusinessExceptions.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessAccessDeniedException(BusinessExceptions.AccessDeniedException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(403, "Access denied");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "You do not have permission to access this resource");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Access denied: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleSpringAccessDeniedException(org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(403, "Access denied");
        response.put("details", "You do not have permission to access this resource");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Spring Security access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(403, "Security violation");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "Security violation detected");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Security exception: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<Map<String, Object>> handleOtpException(OtpException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "OTP operation failed");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "OTP operation failed. Please try again.");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("OTP exception: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitExceededException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(429, "Rate limit exceeded");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "Too many requests. Please try again later.");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Rate limit exceeded for request");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }
    
    @ExceptionHandler({TokenExpiredException.class})
    public ResponseEntity<Map<String, Object>> handleTokenExpiredException(TokenExpiredException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(401, "Token expired");
        response.put("details", "Your session has expired. Please login again.");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Token expired for request");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(ServiceUnavailableException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(503, "Service temporarily unavailable");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "The service is temporarily unavailable. Please try again later.");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Service unavailable for request: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<Map<String, Object>> handleFileUploadException(FileUploadException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "File upload failed");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "The file could not be uploaded. Please check the file and try again.");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("File upload failed: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "Validation failed");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "The provided data failed validation. Please check your input.");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Validation failed: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "Invalid argument");
        response.put("details", sanitizeErrorMessage(ex.getMessage()) != null && !sanitizeErrorMessage(ex.getMessage()).isEmpty() 
            ? sanitizeErrorMessage(ex.getMessage()) : "Invalid argument provided");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));

        logger.warn("Invalid argument provided: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        // Check if it's a business exception that should have been caught earlier
        // Business exceptions extend RuntimeException but should be handled by specific handlers
        if (ex instanceof BusinessExceptions.UserNotFoundException ||
            ex instanceof BusinessExceptions.MerchantNotFoundException ||
            ex instanceof BusinessExceptions.MerchantAlreadyExistsException ||
            ex instanceof BusinessExceptions.UserAlreadyExistsException ||
            ex instanceof BusinessExceptions.InvalidCredentialsException ||
            ex instanceof BusinessExceptions.AccessDeniedException ||
            ex instanceof BusinessExceptions.OtpException ||
            ex instanceof BusinessExceptions.RateLimitExceededException ||
            ex instanceof BusinessExceptions.TokenExpiredException ||
            ex instanceof BusinessExceptions.ServiceUnavailableException ||
            ex instanceof BusinessExceptions.FileUploadException ||
            ex instanceof BusinessExceptions.ValidationException) {
            // Re-throw to be handled by specific handlers
            throw ex;
        }
        
        Map<String, Object> response = ResponseBuilder.error(500, "An unexpected error occurred");
        response.put("details", "An error occurred while processing your request. Please try again later.");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("Runtime exception occurred: {}", sanitizeLogMessage(ex.getMessage()), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        String message = ex.getMessage();
        String errorMessage = "Invalid request format";
        
        if (message != null) {
            if (message.contains("Unrecognized field")) {
                // Extract field name from error message
                String fieldName = extractFieldName(message);
                errorMessage = "Invalid field: " + fieldName;
            } else if (message.contains("JSON parse error")) {
                if (message.contains("merchantId")) {
                    errorMessage = "Invalid data type for merchantId (must be numeric)";
                } else {
                    errorMessage = "Invalid JSON format in request body";
                }
            } else if (message.contains("Required request body is missing")) {
                errorMessage = "Request body is required";
            } else if (message.contains("Content type")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(ResponseBuilder.error(415, "Content-Type must be application/json"));
            }
        }
        
        Map<String, Object> response = ResponseBuilder.error(400, errorMessage);
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Invalid request format detected");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(500, "Internal server error");
        response.put("details", "An unexpected error occurred while processing your request");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("Unexpected exception occurred in request processing");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) return "Invalid request";
        return message.replaceAll("[\r\n\t\f\u0008]", "")
                     .replaceAll("[^\\w\\s._-]", "")
                     .trim();
    }
    
    private String sanitizeLogMessage(String message) {
        if (message == null) return "[NULL]";
        // Prevent log injection by removing CRLF and control characters
        String sanitized = message.replaceAll("[\\r\\n\\t\\f\\u0008\\u0000-\\u001F\\u007F<>\"'&;%${}\\[\\]()]", "_")
                                 .replaceAll("(?i)(script|javascript|vbscript|onload|onerror|eval|exec)", "[FILTERED]")
                                 .replaceAll("[^\\w\\s._-]", "")
                                 .trim();
        return sanitized.length() > 100 ? sanitized.substring(0, 100) + "..." : sanitized;
    }
    

    
    private String extractFieldName(String message) {
        try {
            if (message.contains("Unrecognized field \"")) {
                int start = message.indexOf("Unrecognized field \"") + 19;
                int end = message.indexOf("\"", start);
                if (end > start) {
                    return message.substring(start, end);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract field name from error message");
        }
        return "unknown";
    }
    
    private Object sanitizeRejectedValue(Object value) {
        if (value == null) return null;
        String strValue = value.toString();
        // Mask sensitive fields like passwords, tokens, OTPs
        if (strValue.length() > 50 || 
            strValue.matches(".*(?i)(password|token|otp|secret|key).*")) {
            return "[MASKED]"; 
        }
        // Mask phone numbers (basic check)
        if (strValue.matches("^[6-9]\\d{9}$")) {
            return "****" + strValue.substring(strValue.length() - 4);
        }
        return value;
    }
}