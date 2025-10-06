package com.cloudkitchen.rbac.util;

public final class HttpResponseUtil {
    private HttpResponseUtil() {}
    
    // Success Responses
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int ACCEPTED = 202;
    public static final int NO_CONTENT = 204;
    
    // Client Error Responses 
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int CONFLICT = 409;
    public static final int UNPROCESSABLE_ENTITY = 422;
    public static final int TOO_MANY_REQUESTS = 429;
    
    // Server Error Responses
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;
    
    // Common Response Messages
    public static final String MSG_SUCCESS = "Operation completed successfully";
    public static final String MSG_CREATED = "Resource created successfully";
    public static final String MSG_UPDATED = "Resource updated successfully";
    public static final String MSG_DELETED = "Resource deleted successfully";
    public static final String MSG_NOT_FOUND = "Resource not found";
    public static final String MSG_BAD_REQUEST = "Invalid request parameters";
    public static final String MSG_UNAUTHORIZED = "Authentication required";
    public static final String MSG_FORBIDDEN = "Access denied";
    public static final String MSG_CONFLICT = "Resource already exists";
    public static final String MSG_SERVER_ERROR = "Internal server error";
    public static final String MSG_SERVICE_UNAVAILABLE = "Service temporarily unavailable";
    public static final String MSG_TOO_MANY_REQUESTS = "Too many requests";
}