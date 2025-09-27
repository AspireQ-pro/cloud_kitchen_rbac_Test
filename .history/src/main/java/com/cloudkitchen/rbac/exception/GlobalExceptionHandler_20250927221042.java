package com.cloudkitchen.rbac.exception;

import com.cloudkitchen.rbac.util.ResponseBuilder;
import com.cloudkitchen.rbac.exception.AuthExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "Validation failed");
        
        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getAllErrors().stream()
            .map(err -> {
                FieldError fieldError = (FieldError) err;
                Map<String, Object> error = new HashMap<>();
                error.put("field", fieldError.getField());
                error.put("message", fieldError.getDefaultMessage());
                error.put("rejectedValue", fieldError.getRejectedValue());
                return error;
            })
            .collect(Collectors.toList());
        
        response.put("fieldErrors", fieldErrors);
        response.put("path", request.getDescription(false).replace("uri=", ""));
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
                error.put("rejectedValue", violation.getInvalidValue());
                return error;
            })
            .collect(Collectors.toList());
        
        response.put("fieldErrors", fieldErrors);
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Constraint violation with {} errors", fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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
    
    @ExceptionHandler(AuthExceptions.UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(AuthExceptions.UserNotFoundException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(404, "User not found");
        response.put("details", "No user found with the provided credentials");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("User not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(AuthExceptions.UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(AuthExceptions.UserAlreadyExistsException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "User already exists");
        response.put("details", "The phone number you provided is already registered. Please use a different phone number or try logging in.");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("User registration failed - phone already exists");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(AuthExceptions.InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPasswordException(AuthExceptions.InvalidPasswordException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(401, "Invalid password");
        response.put("details", "The provided password is incorrect");
        response.put("path", request.getDescription(false).replace("uri=", ""));
        response.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Invalid password attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "Invalid request format");
        response.put("details", "The request body contains invalid JSON or missing required fields");
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
        return message.replaceAll("[\r\n\t]", "").trim();
    }
    
    private String sanitizeLogMessage(String message) {
        if (message == null) return "null";
        return message.replaceAll("[\r\n\t]", "");
    }
}