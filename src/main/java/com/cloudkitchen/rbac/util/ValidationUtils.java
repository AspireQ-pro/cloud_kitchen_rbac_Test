package com.cloudkitchen.rbac.util;

import java.util.regex.Pattern;

/**
 * Utility class for input validation and sanitization.
 * <p>
 * This class provides static methods for validating phone numbers, emails,
 * and sanitizing user input to prevent security vulnerabilities.
 * </p>
 *
 * @author Cloud Kitchen RBAC Team
 * @version 1.0
 * @since 1.0
 */
public final class ValidationUtils {
    
    // Indian mobile number pattern: starts with 6-9, followed by 9 digits
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    
    // Standard email pattern with basic validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    // Password strength pattern: min 8 chars, uppercase, lowercase, digit, special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );
    
    private ValidationUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates Indian mobile phone number format.
     * @param phone the phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }
    
    /**
     * Validates email address format.
     * @param email the email address to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validates password strength requirements.
     * @param password the password to validate
     * @return true if password meets strength requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * Sanitizes user input by removing potentially harmful characters.
     * @param input the input string to sanitize
     * @return sanitized string or null if input is null
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\\r\\n\\t\\f\\u0008]", "")
                   .replaceAll("[<>\"'&;%${}\\[\\]()]", "")
                   .trim();
    }
}