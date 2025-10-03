package com.cloudkitchen.rbac.util;

import java.util.regex.Pattern;

public final class SecurityUtils {
    
    // Optimized security patterns with better performance
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script[^>]*>.*?</script>|javascript:|vbscript:|data:|on\\w+\\s*=)", 
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union\\s+select|insert\\s+into|update\\s+set|delete\\s+from|drop\\s+table|create\\s+table|alter\\s+table|exec\\s*\\(|execute\\s*\\(|--|;|'|\"|\\*|%|\\+|or\\s+1\\s*=\\s*1|and\\s+1\\s*=\\s*1|\\bor\\b.*\\b1\\b.*=.*\\b1\\b)", 
        Pattern.CASE_INSENSITIVE);
    

    
    private SecurityUtils() {}
    
    // Validation methods now use ValidationUtils
    
    /**
     * Sanitizes input to prevent XSS attacks
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        
        String sanitized = input.trim()
            .replaceAll("(?i)<script[^>]*>.*?</script>", "")
            .replaceAll("(?i)<[^>]*>", "")
            .replaceAll("[<>\"'&;]", "")
            .replaceAll("javascript:", "")
            .replaceAll("vbscript:", "")
            .replaceAll("(?i)onload", "")
            .replaceAll("(?i)onerror", "")
            .replaceAll("(?i)onclick", "")
            .replaceAll("(?i)onmouseover", "");
            
        // Remove excessive whitespace
        sanitized = sanitized.replaceAll("\\s+", " ");
        
        return sanitized;
    }
    
    /**
     * Checks for potential XSS attacks
     */
    public static boolean containsXSS(String input) {
        return input != null && XSS_PATTERN.matcher(input).find();
    }
    
    /**
     * Checks for potential SQL injection attempts
     */
    public static boolean containsSQLInjection(String input) {
        return input != null && SQL_INJECTION_PATTERN.matcher(input).find();
    }
    
    /**
     * Validates and sanitizes phone number
     */
    public static String sanitizePhone(String phone) {
        if (phone == null) return null;

        // Remove all non-digits
        String cleaned = phone.trim().replaceAll("[^0-9]", "");

        // Check if it's exactly 10 digits and starts with 6-9
        if (cleaned.length() == 10 && ValidationUtils.isValidPhone(cleaned)) {
            return cleaned;
        }

        return null; // Invalid phone number
    }
    
    /**
     * Validates and sanitizes name fields
     */
    public static String sanitizeName(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        // Check for emojis, special characters, and reject them
        if (name.matches(".*[\\uD83C-\\uDBFF\\uDC00-\\uDFFF@#$%^&*()+={}\\[\\]|\\\\:;\"<>?,./~`!].*")) {
            return null;
        }
        
        // Check for SQL injection and XSS in names
        if (containsXSS(name) || containsSQLInjection(name)) {
            return null;
        }

        // Check for extremely long names that could cause database issues
        if (name.length() > 100) {
            return null;
        }

        // Remove all non-alphabetic characters except spaces, hyphens, and apostrophes
        String cleaned = name.trim().replaceAll("[^a-zA-Z\\s\\-']", "");

        // Remove excessive whitespace
        cleaned = cleaned.replaceAll("\\s+", " ");

        // Check length constraints
        if (cleaned.length() < 2 || cleaned.length() > 50) {
            return null;
        }

        return cleaned;
    }
    
    /**
     * Validates and sanitizes address
     */
    public static String sanitizeAddress(String address) {
        if (address == null) return null;

        // Check for XSS and SQL injection
        if (containsXSS(address) || containsSQLInjection(address)) {
            return null;
        }

        // Check for extremely long addresses that could cause database issues
        if (address.length() > 1000) {
            return null;
        }

        String cleaned = sanitizeInput(address);

        // Additional address-specific cleaning
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9\\s\\-,.'#/]", "");

        // Check length constraint
        if (cleaned.length() > 500) {
            return null;
        }

        return cleaned;
    }
    
    /**
     * Validates and sanitizes email
     */
    public static String sanitizeEmail(String email) {
        if (email == null) return null;
        
        String cleaned = email.trim().toLowerCase(java.util.Locale.ROOT);
        
        // Check for XSS and SQL injection
        if (containsXSS(cleaned) || containsSQLInjection(cleaned)) {
            return null;
        }
        
        // Validate email format
        if (!ValidationUtils.isValidEmail(cleaned)) {
            return null;
        }
        
        return cleaned;
    }
    
    /**
     * Masks sensitive data for logging
     */
    public static String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) return "[MASKED]";
        
        // For phone numbers
        if (ValidationUtils.isValidPhone(data)) {
            return "****" + data.substring(data.length() - 4);
        }
        
        // For other sensitive data
        return "[MASKED]";
    }
}