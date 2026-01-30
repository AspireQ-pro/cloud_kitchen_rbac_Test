package com.cloudkitchen.rbac.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.cloudkitchen.rbac.util.ResponseBuilder;

/**
 * Catch-all exception handler for unexpected errors.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GenericExceptionHandler extends ExceptionHandlerSupport {

    private static final Logger logger = LoggerFactory.getLogger(GenericExceptionHandler.class);

    /**
     * Handle unexpected checked exceptions not covered by other handlers.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        Map<String, Object> response = ResponseBuilder.error(500, "Internal server error");
        response.put(DETAILS_KEY, "An unexpected error occurred while processing your request");
        String traceId = addRequestContext(response, request);

        logger.error("Unexpected exception occurred in request processing [{}]", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
