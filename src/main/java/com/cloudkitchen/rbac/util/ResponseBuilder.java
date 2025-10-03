package com.cloudkitchen.rbac.util;

import java.util.Map;

public final class ResponseBuilder {
    
    private ResponseBuilder() {
        // Utility class
    }
    
    public static Map<String, Object> success(int code, String message, Object data) {
        return Map.of(
            "code", code,
            "message", message,
            "success", true,
            "data", data
        );
    }

    public static Map<String, Object> success(int code, String message) {
        return Map.of(
            "code", code,
            "message", message,
            "success", true
        );
    }

    public static Map<String, Object> error(int code, String message) {
        return Map.of(
            "code", code,
            "message", message,
            "success", false
        );
    }
}