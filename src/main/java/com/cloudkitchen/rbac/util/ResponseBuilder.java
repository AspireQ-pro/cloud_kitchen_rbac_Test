package com.cloudkitchen.rbac.util;

import java.util.HashMap;
import java.util.Map;

public final class ResponseBuilder {
    
    private ResponseBuilder() {
        // Utility class
    }
    
    public static Map<String, Object> success(int code, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", code);
        response.put("code", code);
        response.put("message", message);
        response.put("success", true);
        response.put("data", data);
        return response;
    }

    public static Map<String, Object> success(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", code);
        response.put("code", code);
        response.put("message", message);
        response.put("success", true);
        return response;
    }

    public static Map<String, Object> error(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", code);
        response.put("code", code);
        response.put("message", message);
        response.put("success", false);
        return response;
    }
}
