package com.cloudkitchen.rbac.service.impl;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.cloudkitchen.rbac.constants.AppConstants;
import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.service.ValidationService;

@Service
public class ValidationServiceImpl implements ValidationService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern FSSAI_PATTERN = Pattern.compile("^\\d{14}$");

    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int MAX_MERCHANT_NAME_LENGTH = 100;
    
    @Override
    public void validateRegistration(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
        
        validatePhone(request.getPhone());
        validatePassword(request.getPassword());
        
        // Validate firstName
        if (request.getFirstName() == null) {
            throw new IllegalArgumentException("First name is required");
        }
        
        if (request.getFirstName().trim().isEmpty()) {
            if (request.getFirstName().length() > 0) {
                throw new IllegalArgumentException("First name cannot be empty or whitespace only");
            } else {
                throw new IllegalArgumentException("First name is required");
            }
        }
        
        // Validate name contains only valid characters
        if (!request.getFirstName().trim().matches("^[a-zA-Z\\s-]+$")) {
            throw new IllegalArgumentException("Invalid characters in first name");
        }
        
        if (request.getFirstName().trim().length() > 100) {
            throw new IllegalArgumentException("First name too long");
        }
        
        // Validate lastName - now mandatory
        if (request.getLastName() == null) {
            throw new IllegalArgumentException("Last name is required");
        }
        
        if (request.getLastName().trim().isEmpty()) {
            if (request.getLastName().length() > 0) {
                throw new IllegalArgumentException("Last name cannot be empty or whitespace only");
            } else {
                throw new IllegalArgumentException("Last name is required");
            }
        }
        
        if (!request.getLastName().trim().matches("^[a-zA-Z\\s-]+$")) {
            throw new IllegalArgumentException("Invalid characters in last name");
        }
        
        if (request.getLastName().trim().length() > 100) {
            throw new IllegalArgumentException("Last name too long");
        }
    }
    
    @Override
    public void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        
        String trimmedPhone = phone.trim();
        
        // Reject any non-numeric characters (spaces, dashes, alphabets)
        if (!trimmedPhone.matches("^\\d+$")) {
            throw new IllegalArgumentException("Mobile number must be 10 digits");
        }
        
        // Check if phone has exactly 10 digits
        if (trimmedPhone.length() != 10) {
            throw new IllegalArgumentException("Mobile number must be 10 digits");
        }
        
        // Reject leading zero and country codes
        if (trimmedPhone.startsWith("0") || trimmedPhone.startsWith("91")) {
            throw new IllegalArgumentException("Mobile number must be 10 digits");
        }
        
        // Validate Indian mobile number format (starts with 6-9)
        if (!PHONE_PATTERN.matcher(trimmedPhone).matches()) {
            throw new IllegalArgumentException("Mobile number must be 10 digits");
        }
    }
    
    @Override
    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (email.length() > 100) {
            throw new IllegalArgumentException("Email must be 100 characters or less");
        }
    }
    
    @Override
    public void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters with uppercase, lowercase, and digit");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 characters with uppercase, lowercase, and digit");
        }

        if (password.length() > 128) {
            throw new IllegalArgumentException("Password must be 128 characters or less");
        }
    }

    @Override
    public void validateOtp(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP is required");
        }

        String trimmedOtp = otp.trim();

        // Check if OTP contains only numeric characters
        if (!trimmedOtp.matches("^\\d+$")) {
            throw new IllegalArgumentException("Invalid OTP format");
        }

        // Check if OTP is exactly the configured length
        if (trimmedOtp.length() != AppConstants.OTP_LENGTH) {
            throw new IllegalArgumentException("Invalid OTP format");
        }
    }

    @Override
    public void validateMobileForOtp(String mobile) {
        validatePhone(mobile);
    }
    
    @Override
    public void validateMobileForLogin(String mobile) {
        validatePhone(mobile);
    }
    
    @Override
    public void validateTokenFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        String trimmedToken = token.trim();

        // JWT tokens should only contain alphanumeric characters, dots, hyphens, and underscores
        // This prevents SQL injection and XSS attempts
        if (!trimmedToken.matches("^[A-Za-z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid token format");
        }

        // JWT tokens have 3 parts separated by dots
        String[] parts = trimmedToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }

        // Each part should be base64url encoded (no padding)
        for (String part : parts) {
            if (part.isEmpty() || !part.matches("^[A-Za-z0-9_-]+$")) {
                throw new IllegalArgumentException("Invalid token format");
            }
        }

        // Additional length validation to prevent extremely long tokens
        if (trimmedToken.length() > 2048) {
            throw new IllegalArgumentException("Token too long");
        }
    }

    @Override
    public void validateMerchantName(String merchantName) {
        if (merchantName == null || merchantName.trim().isEmpty()) {
            throw new IllegalArgumentException("merchantName is required");
        }

        if (merchantName.trim().length() > MAX_MERCHANT_NAME_LENGTH) {
            throw new IllegalArgumentException("Merchant name must not exceed 100 characters");
        }
    }

    @Override
    public void validateGstin(String gstin) {
        if (gstin == null || gstin.trim().isEmpty()) {
            return;
        }

        String trimmedGstin = gstin.trim();

        if (!GSTIN_PATTERN.matcher(trimmedGstin).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN format");
        }
    }

    @Override
    public void validateFssaiLicense(String fssaiLicense) {
        if (fssaiLicense == null || fssaiLicense.trim().isEmpty()) {
            return;
        }

        String trimmedFssai = fssaiLicense.trim();

        if (!FSSAI_PATTERN.matcher(trimmedFssai).matches()) {
            throw new IllegalArgumentException("Invalid FSSAI license number format");
        }
    }

    @Override
    public void validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return;
        }

        if (address.length() > MAX_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("Address must not exceed 255 characters");
        }
    }
}