package com.cloudkitchen.rbac.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response format")
public class ErrorResponse {
    
    @Schema(description = "HTTP status code", example = "400")
    private String code;
    
    @Schema(description = "Error message", example = "Validation failed")
    private String message;
    
    @Schema(description = "Detailed error description", example = "One or more input fields contain invalid data")
    private String details;
    
    @Schema(description = "Timestamp when error occurred")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request path where error occurred", example = "/api/auth/signup")
    private String path;
    
    @Schema(description = "Validation errors for field-specific issues")
    private List<FieldError> fieldErrors;
    
    @Schema(description = "Trace ID for debugging", example = "abc12345")
    private String traceId;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(String code, String message, String details) {
        this(code, message);
        this.details = details;
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public List<FieldError> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    @Schema(description = "Field-specific validation error")
    public static class FieldError {
        @Schema(description = "Field name", example = "phone")
        private String field;
        
        @Schema(description = "Error message", example = "Invalid phone format")
        private String message;
        
        @Schema(description = "Rejected value", example = "123")
        private Object rejectedValue;

        public FieldError() {}

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getRejectedValue() { return rejectedValue; }
        public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }
    }
}