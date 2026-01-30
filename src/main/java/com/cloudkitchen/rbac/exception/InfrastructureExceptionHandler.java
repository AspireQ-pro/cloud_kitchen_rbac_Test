package com.cloudkitchen.rbac.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.cloudkitchen.rbac.exception.BusinessExceptions.FileUploadException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.ServiceUnavailableException;
import com.cloudkitchen.rbac.util.ResponseBuilder;

/**
 * Exception handler for infrastructure and persistence errors.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InfrastructureExceptionHandler extends ExceptionHandlerSupport {

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureExceptionHandler.class);

    /**
     * Handle database constraint violations with a friendly message.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String message = ex.getMessage();
        String userFriendlyMessage = getUserFriendlyConstraintMessage(message);
        Map<String, Object> response = ResponseBuilder.error(409, userFriendlyMessage);
        String traceId = addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Data integrity violation [{}]: {}", traceId, message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle service availability issues.
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(ServiceUnavailableException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(503, "Service temporarily unavailable");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "The service is temporarily unavailable. Please try again later."));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Service unavailable for request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handle file upload errors.
     */
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<Map<String, Object>> handleFileUploadException(FileUploadException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "File upload failed");
        response.put(DETAILS_KEY, resolveDetails(ex.getMessage(), "The file could not be uploaded. Please check the file and try again."));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "File upload failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Map raw constraint messages to user-facing text.
     */
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
}
