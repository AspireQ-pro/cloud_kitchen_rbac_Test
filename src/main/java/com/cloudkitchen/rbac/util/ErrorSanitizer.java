package com.cloudkitchen.rbac.util;

public final class ErrorSanitizer {

    private ErrorSanitizer() {
        // Utility class
    }

    public static String sanitizeErrorMessage(String message) {
        if (message == null) return "Invalid request";
        return message.replaceAll("[\r\n\t\f\u0008]", "")
                .replaceAll("[^\\w\\s._-]", "")
                .trim();
    }

    public static String sanitizeLogMessage(String message) {
        if (message == null) return "[NULL]";
        // Prevent log injection by removing CRLF and control characters
        String sanitized = message.replaceAll("[\\r\\n\\t\\f\\u0008\\u0000-\\u001F\\u007F<>\"'&;%${}\\[\\]()]", "_")
                .replaceAll("(?i)(script|javascript|vbscript|onload|onerror|eval|exec)", "[FILTERED]")
                .replaceAll("[^\\w\\s._-]", "")
                .trim();
        return sanitized.length() > 100 ? sanitized.substring(0, 100) + "..." : sanitized;
    }

    public static Object sanitizeRejectedValue(Object value) {
        if (value == null) return null;
        String strValue = value.toString();
        // Mask sensitive fields like passwords, tokens, OTPs
        if (strValue.length() > 50 ||
                strValue.matches(".*(?i)(password|token|otp|secret|key).*")) {
            return "[MASKED]";
        }
        // Mask phone numbers (basic check)
        if (strValue.matches("^[6-9]\\d{9}$")) {
            return "****" + strValue.substring(strValue.length() - 4);
        }
        return value;
    }
}
