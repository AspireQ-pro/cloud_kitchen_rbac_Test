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

    /**
     * Validates mobile number format specifically for login operations.
     * @param mobile the mobile number to validate
     * @throws IllegalArgumentException if mobile is invalid
     */
    void validateMobileForLogin(String mobile);

    /**
     * Validates JWT token format to prevent injection attacks.
     * @param token the JWT token to validate
     * @throws IllegalArgumentException if token format is invalid
     */
    void validateTokenFormat(String token);

    /**
     * Validates merchant name format and constraints.
     * @param merchantName the merchant name to validate
     * @throws IllegalArgumentException if merchant name is invalid
     */
    void validateMerchantName(String merchantName);

    /**
     * Validates GSTIN (GST Identification Number) format.
     * @param gstin the GSTIN to validate
     * @throws IllegalArgumentException if GSTIN format is invalid
     */
    void validateGstin(String gstin);

    /**
     * Validates FSSAI license number format.
     * @param fssaiLicense the FSSAI license number to validate
     * @throws IllegalArgumentException if FSSAI license format is invalid
     */
    void validateFssaiLicense(String fssaiLicense);

    /**
     * Validates address length constraints.
     * @param address the address to validate
     * @throws IllegalArgumentException if address is too long
     */
    void validateAddress(String address);
}