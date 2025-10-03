package com.cloudkitchen.rbac.util;

import java.util.regex.Pattern;

public final class ValidationUtils {
    
    // Compiled patterns for performance
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-']{2,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d{4}$");
    
    // Validation methods
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }
    
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidOtp(String otp) {
        return otp != null && OTP_PATTERN.matcher(otp).matches();
    }
    
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    // Validation with error messages
    public static void validatePhone(String phone) {
        if (isBlank(phone)) throw new IllegalArgumentException("Phone number is required");
        if (!isValidPhone(phone)) throw new IllegalArgumentException("Phone must be 10 digits starting with 6-9");
    }
    
    public static void validatePassword(String password) {
        if (isBlank(password)) throw new IllegalArgumentException("Password is required");
        if (!isValidPassword(password)) throw new IllegalArgumentException("Password must be 8-128 chars with uppercase, lowercase, digit, special char");
    }
    
    public static void validateName(String name, String fieldName) {
        if (isBlank(name)) throw new IllegalArgumentException(fieldName + " is required");
        if (!isValidName(name)) throw new IllegalArgumentException("Name must be 2-50 chars, letters only");
    }
    
    public static void validateEmail(String email) {
        if (!isBlank(email) && !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    public static void validateOtp(String otp) {
        if (isBlank(otp)) throw new IllegalArgumentException("OTP is required");
        if (!isValidOtp(otp)) throw new IllegalArgumentException("OTP must be 4 digits");
    }
    
    private ValidationUtils() {} // Prevent instantiation
}