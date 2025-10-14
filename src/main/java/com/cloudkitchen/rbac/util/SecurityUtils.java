package com.cloudkitchen.rbac.util;

public class SecurityUtils {
    
    public static String maskSensitiveData(String data) {
        if (data == null || data.length() < 4) {
            return "****";
        }
        return "****" + data.substring(data.length() - 4);
    }
    
    public static String sanitizeForLogging(String input) {
        if (input == null) {
            return "[NULL]";
        }
        
        // Simple character filtering for log injection prevention
        String sanitized = input.replaceAll("[\\r\\n\\t\\f\\u0008\\u0000-\\u001F\\u007F<>\"'&;%${}\\[\\]()]", "_");
        sanitized = sanitized.replaceAll("(?i)(script|javascript|vbscript|onload|onerror|eval|exec)", "[FILTERED]");
        
        return sanitized.length() > 100 ? sanitized.substring(0, 100) + "..." : sanitized;
    }
    
    public static boolean isValidPhoneFormat(String phone) {
        return phone != null && phone.matches("\\d{10,15}");
    }
    
    public static String cleanPhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.replaceAll("[^0-9]", "");
    }
}