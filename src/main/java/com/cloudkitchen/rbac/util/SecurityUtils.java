package com.cloudkitchen.rbac.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class SecurityUtils {
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror)", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)<[^>]*>|javascript:|vbscript:|onload|onerror|onclick|onmouseover", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PHONE_SANITIZE_PATTERN = Pattern.compile("[^0-9+]");
    private static final Pattern NAME_SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z\\s\\-']");
    private static final Pattern ADDRESS_SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s\\-,.'#/]");

    public static String sanitizeInput(String input) {
        if (input == null) return null;
        
        // Remove control characters and normalize whitespace
        String sanitized = input.replaceAll("[\\r\\n\\t\\f\\v]", "")
                               .replaceAll("\\s+", " ")
                               .trim();
        
        // Check for potential SQL injection
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            throw new IllegalArgumentException("Invalid input detected");
        }
        
        // Check for XSS attempts
        if (XSS_PATTERN.matcher(sanitized).find()) {
            throw new IllegalArgumentException("Invalid input detected");
        }
        
        return sanitized;
    }
    
    public static String sanitizePhone(String phone) {
        if (phone == null) return null;
        return PHONE_SANITIZE_PATTERN.matcher(phone.trim()).replaceAll("");
    }
    
    public static String sanitizeName(String name) {
        if (name == null) return null;
        String sanitized = NAME_SANITIZE_PATTERN.matcher(name.trim()).replaceAll("");
        return sanitized.length() > 50 ? sanitized.substring(0, 50) : sanitized;
    }
    
    public static String sanitizeAddress(String address) {
        if (address == null) return null;
        String sanitized = ADDRESS_SANITIZE_PATTERN.matcher(address.trim()).replaceAll("");
        return sanitized.length() > 500 ? sanitized.substring(0, 500) : sanitized;
    }
    
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
    
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        String cleanPhone = sanitizePhone(phone);
        return cleanPhone.matches("^(\\+91)?[6-9]\\d{9}$");
    }
    
    public static String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) return "****";
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
    
    public static void validateStringLength(String value, String fieldName, int minLength, int maxLength) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (value.trim().length() < minLength) {
            throw new IllegalArgumentException(fieldName + " must be at least " + minLength + " characters");
        }
        if (value.trim().length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " cannot exceed " + maxLength + " characters");
        }
    }
}