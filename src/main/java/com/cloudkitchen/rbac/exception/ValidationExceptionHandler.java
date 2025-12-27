package com.cloudkitchen.rbac.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.cloudkitchen.rbac.util.ErrorSanitizer;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import jakarta.validation.ConstraintViolationException;

/**
 * Exception handler for validation and request parsing errors.
 */
@RestControllerAdvice
public class ValidationExceptionHandler extends ExceptionHandlerSupport {

    private static final Logger logger = LoggerFactory.getLogger(ValidationExceptionHandler.class);

    /**
     * Handle bean validation failures on request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {

        // Collect all required field errors
        List<String> requiredFields = ex.getBindingResult().getAllErrors().stream()
                .filter(FieldError.class::isInstance)
                .map(FieldError.class::cast)
                .filter(err -> {
                    String defaultMessage = err.getDefaultMessage();
                    return defaultMessage != null && defaultMessage.contains("required");
                })
                .map(FieldError::getField)
                .distinct()
                .toList();

        String errorMessage;
        if (requiredFields.size() > 1) {
            errorMessage = String.join(" and ", requiredFields) + " are required";
        } else if (requiredFields.size() == 1) {
            errorMessage = requiredFields.get(0) + " is required";
        } else {
            // Fallback to first validation error
            errorMessage = ex.getBindingResult().getAllErrors().stream()
                    .findFirst()
                    .map(err -> err.getDefaultMessage())
                    .orElse("Validation failed");
        }

        Map<String, Object> response = ResponseBuilder.error(400, errorMessage);

        List<Map<String, Object>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("field", err.getField());
                    error.put("message", err.getDefaultMessage());
                    error.put("rejectedValue", ErrorSanitizer.sanitizeRejectedValue(err.getRejectedValue()));
                    return error;
                })
                .toList();
        if (!fieldErrors.isEmpty()) {
            response.put("fieldErrors", fieldErrors);
        }
        addRequestContext(response, request);

        logger.warn("Validation failed: {}", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle validation failures on request parameters and path variables.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, "Constraint validation failed");

        List<Map<String, Object>> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("field", violation.getPropertyPath().toString());
                    error.put("message", violation.getMessage());
                    error.put("rejectedValue", ErrorSanitizer.sanitizeRejectedValue(violation.getInvalidValue()));
                    return error;
                })
                .toList();

        response.put("fieldErrors", fieldErrors);
        String traceId = addRequestContext(response, request);

        logger.warn("Constraint violation [{}] with {} errors", traceId, fieldErrors.size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle malformed JSON or unreadable request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        String message = ex.getMessage();
        if (isUnsupportedMediaTypeMessage(message)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(ResponseBuilder.error(415, "Content-Type must be application/json"));
        }

        String errorMessage = resolveHttpMessageNotReadableMessage(message, ex);
        Map<String, Object> response = ResponseBuilder.error(400, errorMessage);
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Invalid request format detected: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle unsupported content types for incoming requests.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(415, "Content-Type must be application/json");
        addRequestContext(response, request);

        logger.warn("Unsupported media type: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * Handle invalid argument errors raised by application logic.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(400, resolveDetails(ex.getMessage(), "Invalid argument provided"));
        addRequestContext(response, request);

        warnWithSanitizedMessage(logger, "Invalid argument provided: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private boolean isUnsupportedMediaTypeMessage(String message) {
        return message != null
                && message.contains("Content type")
                && !message.contains("Unrecognized field")
                && !message.contains("JSON parse error")
                && !message.contains("Required request body is missing");
    }

    private String resolveHttpMessageNotReadableMessage(String message, HttpMessageNotReadableException ex) {
        String errorMessage = "Invalid request format";

        if (message != null) {
            if (message.contains("Unrecognized field")) {
                // Extract field name from error message
                String fieldName = extractFieldName(message);
                errorMessage = "Invalid field: " + fieldName;
            } else if (message.contains("JSON parse error")) {
                if (message.contains("merchantId")) {
                    errorMessage = "Invalid data type for merchantId";
                } else {
                    String specificMessage = extractJsonParseDetail(message, ex);
                    errorMessage = specificMessage != null ? specificMessage : "Invalid JSON format in request body";
                }
            } else if (message.contains("Required request body is missing")) {
                errorMessage = "Missing required fields";
            }
        }

        if (isEmptyBodyMessage(message, ex)) {
            errorMessage = "Missing required fields";
        }

        return errorMessage;
    }

    private boolean isEmptyBodyMessage(String message, HttpMessageNotReadableException ex) {
        return message != null && (message.contains("No content to map") ||
                message.contains("Required request body is missing") ||
                ex.getCause() != null && ex.getCause().getMessage() != null &&
                        ex.getCause().getMessage().contains("No content"));
    }

    /**
     * Extract an invalid field name from Jackson error text.
     */
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

    /**
     * Extract a user-facing JSON parse detail when it is not a generic parser error.
     */
    private String extractJsonParseDetail(String message, HttpMessageNotReadableException ex) {
        String detail = null;
        if (message != null) {
            int index = message.indexOf("JSON parse error:");
            if (index >= 0) {
                detail = message.substring(index + "JSON parse error:".length()).trim();
            }
        }
        if ((detail == null || detail.isEmpty()) && ex != null && ex.getMostSpecificCause() != null) {
            detail = ex.getMostSpecificCause().getMessage();
        }
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        String sanitized = ErrorSanitizer.sanitizeErrorMessage(detail);
        if (sanitized.isEmpty()
                || "Invalid request".equalsIgnoreCase(sanitized)
                || isGenericJsonParseError(sanitized)) {
            return null;
        }
        return sanitized;
    }

    /**
     * Detect common Jackson parse errors that should not be shown to clients.
     */
    private boolean isGenericJsonParseError(String message) {
        String lower = message.toLowerCase();
        return lower.contains("cannot deserialize")
                || lower.contains("unexpected character")
                || lower.contains("unrecognized token")
                || lower.contains("unexpected end-of-input")
                || lower.contains("invalid definition")
                || lower.contains("no content")
                || lower.contains("malformed")
                || lower.contains("invalid json")
                || lower.contains("at [source")
                || lower.contains("from string")
                || lower.contains("from number")
                || lower.contains("json parse error");
    }
}
