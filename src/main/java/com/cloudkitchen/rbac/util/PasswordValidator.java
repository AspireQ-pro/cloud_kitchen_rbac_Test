package com.cloudkitchen.rbac.util;

import com.cloudkitchen.rbac.exception.BusinessExceptions.ValidationException;

public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;
    
    public static void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password is required");
        }
        
        if (password.length() < MIN_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_LENGTH + " characters");
        }
        
        if (password.length() > MAX_LENGTH) {
            throw new ValidationException("Password must not exceed " + MAX_LENGTH + " characters");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new ValidationException("Password must contain at least one digit");
        }
        
        if (!password.matches(".*[@$!%*?&].*")) {
            throw new ValidationException("Password must contain at least one special character (@$!%*?&)");
        }
    }
}
