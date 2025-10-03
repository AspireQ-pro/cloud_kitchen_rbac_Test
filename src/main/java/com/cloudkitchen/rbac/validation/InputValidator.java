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
}