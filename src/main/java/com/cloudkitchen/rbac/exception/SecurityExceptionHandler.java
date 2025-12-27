package com.cloudkitchen.rbac.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.cloudkitchen.rbac.exception.BusinessExceptions.AccessDeniedException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.InvalidCredentialsException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.RateLimitExceededException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.TokenExpiredException;
import com.cloudkitchen.rbac.util.ResponseBuilder;

/**
 * Exception handler for authentication, authorization, and security concerns.
 */
@RestControllerAdvice
public class SecurityExceptionHandler extends ExceptionHandlerSupport {

    private static final Logger logger = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    /**
     * Handle invalid login credentials.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentialsException(InvalidCredentialsException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(401, "Authentication failed");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "Invalid credentials provided"));
        addRequestContext(response, request);

        logger.warn("Invalid credentials provided");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle access-denied errors raised by business rules.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(403, "Access denied");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "You do not have permission to access this resource"));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle access-denied errors from Spring Security.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleSpringAccessDeniedException(org.springframework.security.access.AccessDeniedException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(403, "Access denied");
        response.put(DETAILS_KEY, "You do not have permission to access this resource");
        addRequestContext(response, request);

        logger.warn("Spring Security access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle generic security violations.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(403, "Security violation");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "Security violation detected"));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Security exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle rate limit violations.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitExceededException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(429, "Rate limit exceeded");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "Too many requests. Please try again later."));
        addRequestContext(response, request);

        logger.warn("Rate limit exceeded for request");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * Handle expired token errors.
     */
    @ExceptionHandler({TokenExpiredException.class})
    public ResponseEntity<Map<String, Object>> handleTokenExpiredException(TokenExpiredException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(401, "Token expired");
        response.put(DETAILS_KEY, "Your session has expired. Please login again.");
        addRequestContext(response, request);

        logger.warn("Token expired for request");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
