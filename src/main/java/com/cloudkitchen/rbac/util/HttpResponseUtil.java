package com.cloudkitchen.rbac.util;

public final class HttpResponseUtil {
    private HttpResponseUtil() {}
    
    // Success Responses
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int ACCEPTED = 202;
    public static final int NO_CONTENT = 204;
    public static final int PARTIAL_CONTENT = 206;
    
    // Redirection Responses
    public static final int NOT_MODIFIED = 304;
    
    // Client Error Responses 
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int PAYMENT_REQUIRED = 402;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int NOT_ACCEPTABLE = 406;
    public static final int REQUEST_TIMEOUT = 408;
    public static final int CONFLICT = 409;
    public static final int GONE = 410;
    public static final int LENGTH_REQUIRED = 411;
    public static final int PRECONDITION_FAILED = 412;
    public static final int PAYLOAD_TOO_LARGE = 413;
    public static final int URI_TOO_LONG = 414;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int UNPROCESSABLE_ENTITY = 422;
    public static final int LOCKED = 423;
    public static final int TOO_MANY_REQUESTS = 429;
    
    // Server Error Responses
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int NOT_IMPLEMENTED = 501;
    public static final int BAD_GATEWAY = 502;
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final int GATEWAY_TIMEOUT = 504;
    public static final int INSUFFICIENT_STORAGE = 507;
    
    // Common Response Messages
    public static final String MSG_SUCCESS = "Operation completed successfully";
    public static final String MSG_CREATED = "Resource created successfully";
    public static final String MSG_UPDATED = "Resource updated successfully";
    public static final String MSG_DELETED = "Resource deleted successfully";
    public static final String MSG_ACCEPTED = "Request accepted for processing";
    public static final String MSG_NO_CONTENT = "No content to return";
    public static final String MSG_PARTIAL_CONTENT = "Partial content returned";
    
    // Error Messages
    public static final String MSG_NOT_FOUND = "Resource not found";
    public static final String MSG_BAD_REQUEST = "Invalid request parameters";
    public static final String MSG_UNAUTHORIZED = "Authentication required";
    public static final String MSG_PAYMENT_REQUIRED = "Payment required";
    public static final String MSG_FORBIDDEN = "Access denied";
    public static final String MSG_METHOD_NOT_ALLOWED = "Method not allowed";
    public static final String MSG_NOT_ACCEPTABLE = "Not acceptable";
    public static final String MSG_REQUEST_TIMEOUT = "Request timeout";
    public static final String MSG_CONFLICT = "Resource already exists";
    public static final String MSG_GONE = "Resource no longer available";
    public static final String MSG_LENGTH_REQUIRED = "Content length required";
    public static final String MSG_PRECONDITION_FAILED = "Precondition failed";
    public static final String MSG_PAYLOAD_TOO_LARGE = "Request payload too large";
    public static final String MSG_URI_TOO_LONG = "URI too long";
    public static final String MSG_UNSUPPORTED_MEDIA_TYPE = "Unsupported media type";
    public static final String MSG_UNPROCESSABLE_ENTITY = "Unprocessable entity";
    public static final String MSG_LOCKED = "Resource locked";
    public static final String MSG_TOO_MANY_REQUESTS = "Too many requests";
    
    // Server Error Messages
    public static final String MSG_SERVER_ERROR = "Internal server error";
    public static final String MSG_NOT_IMPLEMENTED = "Not implemented";
    public static final String MSG_BAD_GATEWAY = "Bad gateway";
    public static final String MSG_SERVICE_UNAVAILABLE = "Service temporarily unavailable";
    public static final String MSG_GATEWAY_TIMEOUT = "Gateway timeout";
    public static final String MSG_INSUFFICIENT_STORAGE = "Insufficient storage";
}