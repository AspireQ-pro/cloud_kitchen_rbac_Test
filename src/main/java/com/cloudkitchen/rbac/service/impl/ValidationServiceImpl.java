package com.cloudkitchen.rbac.service.impl;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

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
        
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            validateEmail(request.getEmail());
        }
        
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        
        if (request.getFirstName().length() > 50) {
            throw new IllegalArgumentException("First name must be 50 characters or less");
        }
        
        if (request.getLastName().length() > 50) {
            throw new IllegalArgumentException("Last name must be 50 characters or less");
        }
    }
    
    @Override
    public void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        
        String cleanPhone = phone.trim().replaceAll("[^0-9]", "");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new IllegalArgumentException("Invalid phone number format. Must be 10 digits starting with 6-9");
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
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 characters with uppercase, lowercase, digit, and special character");
        }
        
        if (password.length() > 128) {
            throw new IllegalArgumentException("Password must be 128 characters or less");
        }
    }
}