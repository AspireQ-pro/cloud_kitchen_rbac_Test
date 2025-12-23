package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.service.impl.ValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {
    
    private ValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new ValidationServiceImpl();
    }
    
    @Test
    void testBlankFirstNameValidation() {
        RegisterRequest request = createValidRequest();
        request.setFirstName("");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validationService.validateRegistration(request));
        assertEquals("First name is required", exception.getMessage());
    }
    
    @Test
    void testBlankMobileNumberValidation() {
        RegisterRequest request = createValidRequest();
        request.setPhone("");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validationService.validateRegistration(request));
        assertEquals("Mobile number is required", exception.getMessage());
    }
    
    @Test
    void testInvalidMobileNumberFormat() {
        RegisterRequest request = createValidRequest();
        request.setPhone("9465909292a");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validationService.validateRegistration(request));
        assertEquals("Invalid mobile number", exception.getMessage());
    }
    
    @Test
    void testMissingLastName() {
        RegisterRequest request = createValidRequest();
        request.setLastName("");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validationService.validateRegistration(request));
        assertEquals("Last name is required", exception.getMessage());
    }
    
    @Test
    void testWhitespaceInFields() {
        RegisterRequest request = createValidRequest();
        request.setFirstName("   ");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validationService.validateRegistration(request));
        assertEquals("First name cannot be empty or whitespace only", exception.getMessage());
    }
    
    @Test
    void testWeakPassword() {
        RegisterRequest request = createValidRequest();
        request.setPassword("Abc123");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> validationService.validateRegistration(request));
        assertEquals("Password must be at least 8 characters with uppercase, lowercase, and digit", exception.getMessage());
    }
    
    @Test
    void testValidRegistration() {
        RegisterRequest request = createValidRequest();
        
        assertDoesNotThrow(() -> validationService.validateRegistration(request));
    }
    
    private RegisterRequest createValidRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setMerchantId(1);
        request.setPhone("9876543210");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setAddress("123 Main Street");
        return request;
    }
}