package com.cloudkitchen.rbac.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer Management", description = "Customer data retrieval operations")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Get All Customers", description = "Super Admin: Get all customers. Merchant: Get only their own customers")
    public ResponseEntity<Map<String, Object>> getAllCustomers(Authentication authentication) {
        try {
            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_SUPER_ADMIN".equals(auth.getAuthority()));
            
            List<CustomerResponse> response;
            
            if (isSuperAdmin) {
                // Super Admin gets all customers
                response = customerService.getAllCustomers(authentication);
            } else if (customerService.canAccessCustomers(authentication)) {
                // Merchant gets only their own customers
                Integer merchantId = customerService.getMerchantIdFromAuth(authentication);
                response = customerService.getCustomersByMerchantId(merchantId);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, "Access denied"));
            }
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Customers retrieved successfully. Total: " + response.size(), response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving customers"));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Customer by ID", description = "Get a specific customer by their ID")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Integer id, Authentication authentication) {
        try {
            // Check permissions
            boolean hasAccess = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_SUPER_ADMIN".equals(auth.getAuthority()) || 
                                    "customer.read".equals(auth.getAuthority())) ||
                    customerService.canAccessCustomer(authentication, id);
            
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, "Access denied"));
            }
            
            CustomerResponse response = customerService.getCustomerById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Customer profile retrieved successfully", response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "Customer not found with ID: " + id));
            }
            if (e.getMessage().contains("not a customer")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "User is not a customer"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving customer"));
        }
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get Customers by Merchant", description = "Get all customers for a specific merchant")
    public ResponseEntity<Map<String, Object>> getCustomersByMerchant(@PathVariable Integer merchantId, Authentication authentication) {
        try {
            // Check permissions
            boolean hasAccess = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_SUPER_ADMIN".equals(auth.getAuthority()) || 
                                    "customer.read".equals(auth.getAuthority())) ||
                    customerService.canAccessMerchantCustomers(authentication, merchantId);
            
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, "Access denied"));
            }
            
            List<CustomerResponse> response = customerService.getCustomersByMerchantId(merchantId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Customers for merchant ID " + merchantId + " retrieved successfully. Total: " + response.size(), response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Merchant not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "Merchant not found with ID: " + merchantId));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving customers for merchant"));
        }
    }
}