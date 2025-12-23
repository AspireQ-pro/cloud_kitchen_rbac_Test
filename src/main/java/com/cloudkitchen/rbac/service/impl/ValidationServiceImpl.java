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
        
        // Check for alphabetic characters in phone
        if (trimmedPhone.matches(".*[a-zA-Z].*")) {
            throw new IllegalArgumentException("Invalid mobile number");
        }
        
        // Remove all non-digit characters for validation
        String cleanPhone = trimmedPhone.replaceAll("[^0-9]", "");
        
        // Check if phone has exactly 10 digits
        if (cleanPhone.length() != 10) {
            throw new IllegalArgumentException("Mobile number must be exactly 10 digits");
        }
        
        // Validate Indian mobile number format
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new IllegalArgumentException("Invalid mobile number format");
        }
        
        // Additional check: if original contains non-digits other than spaces/hyphens, reject
        if (!trimmedPhone.matches("^[0-9\\s\\-+()]*$")) {
            throw new IllegalArgumentException("Invalid mobile number");
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
        if (mobile == null || mobile.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }

        String trimmedMobile = mobile.trim();

        // Remove all non-digit characters for validation
        String cleanMobile = trimmedMobile.replaceAll("[^0-9]", "");

        // Check for alphabetic characters in mobile
        if (trimmedMobile.matches(".*[a-zA-Z].*")) {
            throw new IllegalArgumentException("Invalid mobile number format");
        }

        // Check if mobile has exactly 10 digits
        if (cleanMobile.length() != 10) {
            throw new IllegalArgumentException("Invalid mobile number format");
        }

        // Validate Indian mobile number format
        if (!PHONE_PATTERN.matcher(cleanMobile).matches()) {
            throw new IllegalArgumentException("Invalid mobile number format");
        }
    }
}