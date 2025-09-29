package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;
    private final JwtTokenProvider jwt;

    public CustomerController(CustomerService customerService, JwtTokenProvider jwt) {
        this.customerService = customerService;
        this.jwt = jwt;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Profile retrieved successfully", customerService.getCustomerById(userId, userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ResponseBuilder.error(500, "Internal server error"));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCustomers(@RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customers retrieved successfully", customerService.getAllCustomers(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ResponseBuilder.error(500, "Internal server error"));
        }
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<Map<String, Object>> getCustomersByMerchant(@PathVariable Integer merchantId, @RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customers retrieved successfully", customerService.getCustomersByMerchantId(merchantId, userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        }
    }
    
    @GetMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Integer customerId, @RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customer retrieved successfully", customerService.getCustomerById(customerId, userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        }
    }
    
    @PutMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> updateCustomer(@PathVariable Integer customerId, @Valid @RequestBody CustomerResponse req, @RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customer updated successfully", customerService.updateCustomer(customerId, req, userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        }
    }
    
    private Integer extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        try {
            return jwt.getUserIdFromToken(authHeader.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }
}