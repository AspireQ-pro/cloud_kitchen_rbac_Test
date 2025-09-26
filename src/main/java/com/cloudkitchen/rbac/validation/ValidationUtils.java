package com.cloudkitchen.rbac.validation;

import com.cloudkitchen.rbac.constants.ErrorCodes;
import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+91)?[6-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-]{2,50}$");
    
    public static void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException(ErrorCodes.REQUIRED_FIELD_MISSING + ": Phone is required");
        }
        String sanitizedPhone = sanitizeInput(phone.replaceAll("\\s+", ""));
        if (!PHONE_PATTERN.matcher(sanitizedPhone).matches()) {
            throw new IllegalArgumentException(ErrorCodes.INVALID_PHONE + ": Invalid phone format");
        }
    }
    
    public static void validateEmail(String email) {
        if (email != null && !email.isBlank()) {
            String sanitizedEmail = sanitizeInput(email);
            if (!EMAIL_PATTERN.matcher(sanitizedEmail).matches()) {
                throw new IllegalArgumentException(ErrorCodes.INVALID_EMAIL + ": Invalid email format");
            }
        }
    }
    
    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be 8+ chars with upper, lower, digit, special char");
        }
    }
    
    public static void validateRequiredField(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
    }
    
    public static void validateName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(ErrorCodes.REQUIRED_FIELD_MISSING + ": " + fieldName + " is required");
        }
        String sanitizedName = sanitizeInput(name.trim());
        if (sanitizedName.length() < 2 || sanitizedName.length() > 50) {
            throw new IllegalArgumentException(ErrorCodes.INVALID_FORMAT + ": " + fieldName + " must be 2-50 characters");
        }
        if (!NAME_PATTERN.matcher(sanitizedName).matches()) {
            throw new IllegalArgumentException(ErrorCodes.INVALID_FORMAT + ": " + fieldName + " invalid format");
        }
    }
    
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[\r\n\t<>\"'&]", "").trim();
    }
    
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}