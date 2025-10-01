package com.cloudkitchen.rbac.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @Operation(summary = "Get All Customers", description = "Get all customers")
    @PreAuthorize("hasRole('super_admin') or hasPermission('customer.read')")
    public ResponseEntity<Map<String, Object>> getAllCustomers() {
        try {
            List<CustomerResponse> response = customerService.getAllCustomers();
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customers retrieved successfully", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Failed to retrieve customers"));
        }
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get Customers by Merchant", description = "Get all customers for a specific merchant")
    public ResponseEntity<Map<String, Object>> getCustomersByMerchant(@PathVariable Integer merchantId) {
        try {
            List<CustomerResponse> response = customerService.getCustomersByMerchantId(merchantId);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customers retrieved successfully", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Failed to retrieve customers"));
        }
    }
}