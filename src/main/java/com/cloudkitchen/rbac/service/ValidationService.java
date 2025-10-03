package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.util.SecurityUtils;
import com.cloudkitchen.rbac.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    public void validateRegistration(RegisterRequest req) {
        log.debug("Starting comprehensive registration validation for phone: {}",
                 req.getPhone() != null ? SecurityUtils.maskSensitiveData(req.getPhone()) : "null");

        // Basic field validation
        validateRequiredFields(req);

        // Format validation
        validateFieldFormats(req);

        // Security validation
        validateSecurity(req);

        // Length validation
        validateLengths(req);

        log.debug("Registration validation completed successfully");
    }

    /**
     * Validate required fields are present and not empty
     */
    private void validateRequiredFields(RegisterRequest req) {
        ValidationUtils.validateName(req.getFirstName(), "First name");
        ValidationUtils.validateName(req.getLastName(), "Last name");
        ValidationUtils.validatePhone(req.getPhone());
        ValidationUtils.validatePassword(req.getPassword());

        if (req.getMerchantId() == null || req.getMerchantId() <= 0) {
            throw new IllegalArgumentException("Valid merchantId (>0) is required for customer registration");
        }
    }

    /**
     * Validate field formats using centralized patterns
     */
    private void validateFieldFormats(RegisterRequest req) {
        // Email format validation (if provided)
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            ValidationUtils.validateEmail(req.getEmail());
        }
    }

    /**
     * Validate security aspects (XSS, SQL injection, etc.)
     */
    private void validateSecurity(RegisterRequest req) {
        // Check names for XSS and SQL injection
        if (SecurityUtils.containsXSS(req.getFirstName()) || SecurityUtils.containsSQLInjection(req.getFirstName())) {
            log.warn("XSS/SQL injection attempt detected in firstName");
            throw new IllegalArgumentException("First name contains invalid characters");
        }

        if (SecurityUtils.containsXSS(req.getLastName()) || SecurityUtils.containsSQLInjection(req.getLastName())) {
            log.warn("XSS/SQL injection attempt detected in lastName");
            throw new IllegalArgumentException("Last name contains invalid characters");
        }

        // Check address for XSS and SQL injection (if provided)
        if (req.getAddress() != null && !req.getAddress().trim().isEmpty()) {
            if (SecurityUtils.containsXSS(req.getAddress()) || SecurityUtils.containsSQLInjection(req.getAddress())) {
                log.warn("XSS/SQL injection attempt detected in address");
                throw new IllegalArgumentException("Address contains invalid characters");
            }
        }

        // Check email for XSS and SQL injection (if provided)
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            if (SecurityUtils.containsXSS(req.getEmail()) || SecurityUtils.containsSQLInjection(req.getEmail())) {
                log.warn("XSS/SQL injection attempt detected in email");
                throw new IllegalArgumentException("Email contains invalid characters");
            }
        }
    }

    /**
     * Validate field lengths to prevent database issues
     */
    private void validateLengths(RegisterRequest req) {
        // Check for extremely long names that could cause database issues
        if (req.getFirstName() != null && req.getFirstName().length() > 100) {
            throw new IllegalArgumentException("First name is too long");
        }

        if (req.getLastName() != null && req.getLastName().length() > 100) {
            throw new IllegalArgumentException("Last name is too long");
        }

        // Check address length
        if (req.getAddress() != null && req.getAddress().length() > 1000) {
            throw new IllegalArgumentException("Address is too long");
        }
    }

    /**
     * Validate phone number format (for OTP and other services)
     */
    public void validatePhone(String phone) {
        ValidationUtils.validatePhone(phone);
    }
}