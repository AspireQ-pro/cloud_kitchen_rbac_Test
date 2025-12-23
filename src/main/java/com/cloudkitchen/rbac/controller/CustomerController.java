package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerUpdateRequest request,
            Authentication authentication) {
        if (!customerService.canAccessCustomer(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(403, "Access denied. Customers can only update their own profile."));
        }
        try {
            Integer updatedBy = authentication != null ? Integer.valueOf(authentication.getName()) : null;
            CustomerResponse response = customerService.updateCustomer(id, request, updatedBy);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customer profile updated successfully", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, "Failed to update customer: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        if (!customerService.canAccessCustomers(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(403, "Access denied. Only merchants and admins can view customer lists."));
        }
        try {
            PageRequest pageRequest = new PageRequest(page, size);
            PageResponse<CustomerResponse> customers = customerService.getAllCustomers(pageRequest, status, search, authentication);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customers retrieved successfully", customers));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(403, "Access denied. Only merchants and admins can view customer lists."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable Integer id, Authentication authentication) {
        if (!customerService.canAccessCustomer(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(403, "Access denied. Customers can only view their own profile."));
        }
        try {
            CustomerResponse customer = customerService.getCustomerById(id);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customer details retrieved successfully", customer));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(404, "Customer not found"));
        }
    }
    
    @GetMapping("/profile")
    public ResponseEntity<?> getCustomerProfile(Authentication authentication) {
        try {
            CustomerResponse customer = customerService.getCustomerProfile(authentication);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Profile retrieved successfully", customer));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(404, "Profile not found"));
        }
    }
    
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<?> getCustomersByMerchantId(
            @PathVariable Integer merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        if (!customerService.canAccessMerchantCustomers(authentication, merchantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(403, "Access denied. Customers cannot view other customers. Merchants can only view their own customers."));
        }
        try {
            PageRequest pageRequest = new PageRequest(page, size);
            PageResponse<CustomerResponse> customers = customerService.getCustomersByMerchantId(merchantId, pageRequest);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Merchant customers retrieved successfully", customers));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, "Failed to retrieve customers: " + e.getMessage()));
        }
    }
}