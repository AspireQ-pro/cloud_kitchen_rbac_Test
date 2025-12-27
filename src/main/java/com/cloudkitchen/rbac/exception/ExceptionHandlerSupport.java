package com.cloudkitchen.rbac.exception;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.web.context.request.WebRequest;

import com.cloudkitchen.rbac.util.ErrorSanitizer;

abstract class ExceptionHandlerSupport {
    protected static final String DETAILS_KEY = "details";

    protected String addRequestContext(Map<String, Object> response, WebRequest request) {
        response.put("path", request.getDescription(false).replace("uri=", ""));
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        response.put("traceId", traceId);
        return traceId;
    }

    protected String resolveDetails(String rawMessage, String fallback) {
        String sanitized = ErrorSanitizer.sanitizeErrorMessage(rawMessage);
        return sanitized.isEmpty() ? fallback : sanitized;
    }

    protected void warnWithSanitizedMessage(Logger logger, String template, String rawMessage) {
        if (logger.isWarnEnabled()) {
            logger.warn(template, ErrorSanitizer.sanitizeLogMessage(rawMessage));
        }
    }

    protected void warnWithSanitizedMessage(Logger logger, String template, Object firstArg, String rawMessage) {
        if (logger.isWarnEnabled()) {
            logger.warn(template, firstArg, ErrorSanitizer.sanitizeLogMessage(rawMessage));
        }
    }
}
