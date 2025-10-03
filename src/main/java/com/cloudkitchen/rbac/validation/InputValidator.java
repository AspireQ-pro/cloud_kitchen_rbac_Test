package com.cloudkitchen.rbac.validation;

import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.service.ValidationService;
import com.cloudkitchen.rbac.util.SecurityUtils;
import org.springframework.stereotype.Component;

@Component
public final class InputValidator {

    private final ValidationService validationService;

    public InputValidator(ValidationService validationService) {
        this.validationService = validationService;
    }

    public void validateRegistration(RegisterRequest req) {
        validationService.validateRegistration(req);
    }
    
    private void validateSecurity(RegisterRequest req) {
        if (SecurityUtils.containsXSS(req.getFirstName()) || SecurityUtils.containsSQLInjection(req.getFirstName())) {
            throw new IllegalArgumentException("Name contains invalid characters");
        }
        if (SecurityUtils.containsXSS(req.getLastName()) || SecurityUtils.containsSQLInjection(req.getLastName())) {
            throw new IllegalArgumentException("Name contains invalid characters");
        }
        if (req.getAddress() != null && (SecurityUtils.containsXSS(req.getAddress()) || SecurityUtils.containsSQLInjection(req.getAddress()))) {
            throw new IllegalArgumentException("Address contains invalid characters");
        }
    }
}