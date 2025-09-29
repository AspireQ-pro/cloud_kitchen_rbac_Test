package com.cloudkitchen.rbac.constants;

public final class ValidationMessages {
    
    // Field Required Messages
    public static final String PHONE_REQUIRED = "Phone number is required";
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String FIRST_NAME_REQUIRED = "First name is required";
    public static final String LAST_NAME_REQUIRED = "Last name is required";
    public static final String EMAIL_REQUIRED = "Email is required";
    
    // Format Validation Messages
    public static final String PHONE_FORMAT = "Phone must be 10 digits starting with 6-9";
    public static final String PASSWORD_FORMAT = "Password must be 8-128 chars with uppercase, lowercase, digit, special char";
    public static final String NAME_FORMAT = "Name must be 2-50 chars, letters only";
    public static final String EMAIL_FORMAT = "Invalid email format";
    public static final String ADDRESS_FORMAT = "Address contains invalid characters";
    
    // Size Validation Messages
    public static final String PHONE_SIZE = "Phone must be exactly 10 digits";
    public static final String PASSWORD_SIZE = "Password must be 8-128 characters";
    public static final String NAME_SIZE = "Name must be 2-50 characters";
    public static final String EMAIL_SIZE = "Email max 100 characters";
    public static final String ADDRESS_SIZE = "Address max 500 characters";
    
    // Business Logic Messages
    public static final String USER_NOT_FOUND = "User not found";
    public static final String USER_ALREADY_EXISTS = "User already exists";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String MERCHANT_NOT_FOUND = "Merchant not found";
    
    // Prevent instantiation
    private ValidationMessages() {}
}