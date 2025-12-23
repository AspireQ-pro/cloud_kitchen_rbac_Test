package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.auth.RegisterRequest;

/**
 * Service interface for validation operations.
 * Provides methods to validate user input data including registration details,
 * phone numbers, email addresses, and passwords.
 */
public interface ValidationService {
    
    /**
     * Validates user registration request data.
     * @param request the registration request to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateRegistration(RegisterRequest request);
    
    /**
     * Validates phone number format and constraints.
     * @param phone the phone number to validate
     * @throws IllegalArgumentException if phone is invalid
     */
    void validatePhone(String phone);
    
    /**
     * Validates email address format and constraints.
     * @param email the email address to validate
     * @throws IllegalArgumentException if email is invalid
     */
    void validateEmail(String email);
    
    /**
     * Validates password strength and security requirements.
     * @param password the password to validate
     * @throws IllegalArgumentException if password is weak
     */
    void validatePassword(String password);

    /**
     * Validates OTP format and constraints.
     * @param otp the OTP to validate
     * @throws IllegalArgumentException if OTP is invalid
     */
    void validateOtp(String otp);

    /**
     * Validates mobile number format specifically for OTP operations.
     * @param mobile the mobile number to validate
     * @throws IllegalArgumentException if mobile is invalid
     */
    void validateMobileForOtp(String mobile);
}