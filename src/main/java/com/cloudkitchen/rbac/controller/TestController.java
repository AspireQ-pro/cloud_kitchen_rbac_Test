package com.cloudkitchen.rbac.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.dto.auth.AuthRequest;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    private final AuthService authService;
    
    public TestController(AuthService authService) {
        this.authService = authService;
    }
    
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getTestUsers() {
        try {
            long userCount = authService.getUserCount();
            
            Map<String, Object> testUsers = Map.of(
                "superadmin", Map.of(
                    "username", "superadmin",
                    "password", "Admin123!",
                    "merchantId", 0,
                    "description", "Super admin login"
                ),
                "merchantadmin", Map.of(
                    "username", "merchantadmin", 
                    "password", "Merchant123!",
                    "merchantId", 0,
                    "description", "Merchant admin login"
                ),
                "totalUsers", userCount
            );
            
            return ResponseEntity.ok(ResponseBuilder.success(200, "Test users available", testUsers));
        } catch (Exception e) {
            log.error("Error getting test users: {}", e.getMessage());
            return ResponseEntity.ok(ResponseBuilder.error(500, "Error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login-superadmin")
    public ResponseEntity<Map<String, Object>> testSuperAdminLogin() {
        try {
            AuthRequest req = new AuthRequest();
            req.setUsername("superadmin");
            req.setPassword("Admin123!");
            req.setMerchantId(0);
            
            var response = authService.login(req);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Super admin login successful", response));
        } catch (Exception e) {
            log.error("Super admin login test failed: {}", e.getMessage());
            return ResponseEntity.ok(ResponseBuilder.error(500, "Login failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login-merchant")
    public ResponseEntity<Map<String, Object>> testMerchantLogin() {
        try {
            AuthRequest req = new AuthRequest();
            req.setUsername("merchantadmin");
            req.setPassword("Merchant123!");
            req.setMerchantId(0);
            
            var response = authService.login(req);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Merchant admin login successful", response));
        } catch (Exception e) {
            log.error("Merchant admin login test failed: {}", e.getMessage());
            return ResponseEntity.ok(ResponseBuilder.error(500, "Login failed: " + e.getMessage()));
        }
    }
}