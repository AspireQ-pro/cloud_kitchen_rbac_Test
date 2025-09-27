package com.cloudkitchen.rbac.exception;

import com.cloudkitchen.rbac.dto.ErrorResponse;
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
import java.sql.SQLException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("400", "Validation failed", "One or more input fields contain invalid data");
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getAllErrors().stream()
            .map(err -> {
                FieldError fieldError = (FieldError) err;
                return new ErrorResponse.FieldError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()
                );
            })
            .collect(Collectors.toList());
        
        error.setFieldErrors(fieldErrors);
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Validation failed with {} errors", fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("400", "Constraint validation failed", "Request parameters violate defined constraints");
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
            .map(violation -> new ErrorResponse.FieldError(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue()
            ))
            .collect(Collectors.toList());
        
        error.setFieldErrors(fieldErrors);
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Constraint violation with {} errors", fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        ErrorResponse error = new ErrorResponse("400", "Invalid parameter type", 
            String.format("Parameter '%s' expects %s but received invalid value", ex.getName(), expectedType));
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Type mismatch for parameter {}: expected {}", ex.getName(), expectedType);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("401", "Invalid credentials", 
            "The provided phone number or password is incorrect");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Authentication failed: invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("401", "Authentication failed", 
            "Unable to authenticate user with provided credentials");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Authentication exception: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("403", "Access denied", 
            "You do not have sufficient permissions to access this resource");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Access denied for path: {}", request.getDescription(false));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("400", "Invalid argument", 
            sanitizeErrorMessage(ex.getMessage()));
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Invalid argument: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    // Auth Exception Handlers
    @ExceptionHandler(AuthExceptions.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(AuthExceptions.UserNotFoundException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("404", "User not found", 
            "No user found with the provided credentials");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("User not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(AuthExceptions.UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(AuthExceptions.UserAlreadyExistsException ex, WebRequest request) {
        // ERR_4001 maps to HTTP 400 Bad Request - Client provided invalid data (duplicate phone)
        ErrorResponse error = new ErrorResponse("400", "User already exists", 
            "The phone number you provided is already registered. Please use a different phone number or try logging in.");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("User registration failed - phone already exists");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(AuthExceptions.InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordException(AuthExceptions.InvalidPasswordException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("401", "Invalid password", 
            "The provided password is incorrect");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Invalid password attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    @ExceptionHandler(AuthExceptions.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthAccessDeniedException(AuthExceptions.AccessDeniedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("403", "Access denied", 
            "You are not authorized to perform this action");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Access denied: {}", sanitizeLogMessage(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String message = ex.getMessage();
        
        // Handle specific runtime exceptions
        if (message != null && message.contains("Failed to send OTP")) {
            ErrorResponse error = new ErrorResponse("503", "OTP sending failed", 
                "Unable to send OTP to the provided phone number");
            error.setPath(request.getDescription(false).replace("uri=", ""));
            error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
            logger.error("OTP sending failed");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }
        
        ErrorResponse error = new ErrorResponse("500", "Internal server error", 
            "An unexpected error occurred while processing your request");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("Runtime exception in {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    // HTTP Method and Media Type Errors
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("405", "Method not allowed", 
            String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()));
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Method not allowed: {} for {}", ex.getMethod(), request.getDescription(false));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }
    
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("415", "Unsupported media type", 
            String.format("Content type '%s' is not supported", ex.getContentType()));
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Unsupported media type: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }
    
    // Database Errors
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("500", "Database error", 
            "A database error occurred while processing your request");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("Database error in {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(SQLException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("500", "Database error", 
            "A database error occurred while processing your request");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("SQL error in {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    // Additional HTTP Status Code Handlers
    
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(org.springframework.web.bind.MissingServletRequestParameterException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("400", "Missing required parameter", 
            String.format("Required parameter '%s' is missing", ex.getParameterName()));
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Missing required parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("400", "Missing required header", 
            String.format("Required header '%s' is missing", ex.getHeaderName()));
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Missing required header: {}", ex.getHeaderName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(org.springframework.web.multipart.MaxUploadSizeExceededException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("413", "Payload too large", 
            "The uploaded file size exceeds the maximum allowed limit");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("File upload size exceeded");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }
    
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("405", "Method not allowed", 
            String.format("HTTP method '%s' is not supported. Supported methods: %s", 
                ex.getMethod(), String.join(", ", ex.getSupportedMethods())));
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Method not allowed: {}", ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }
    
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(org.springframework.web.servlet.NoHandlerFoundException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("404", "Endpoint not found", 
            String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()));
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(java.util.concurrent.TimeoutException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("408", "Request timeout", 
            "The request took too long to process and has timed out");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("Request timeout");
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(error);
    }
    
    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleFileAccessDenied(java.nio.file.AccessDeniedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("403", "File access denied", 
            "Access to the requested file or directory is denied");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.warn("File access denied: {}", ex.getFile());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(java.net.ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectionError(java.net.ConnectException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("502", "Bad gateway", 
            "Unable to connect to external service");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("Connection error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }
    
    @ExceptionHandler(java.net.SocketTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleSocketTimeout(java.net.SocketTimeoutException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("504", "Gateway timeout", 
            "The external service did not respond within the expected time");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("Socket timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse("500", "Internal server error", 
            "An unexpected error occurred while processing your request");
        
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setTraceId(UUID.randomUUID().toString().substring(0, 8));
        
        logger.error("Unexpected exception in {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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