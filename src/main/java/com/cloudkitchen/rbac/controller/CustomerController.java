package com.cloudkitchen.rbac.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.AccessControlUtil;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Customer data retrieval operations")
public class CustomerController {
    private final CustomerService customerService;
    private final AccessControlUtil accessControl;

    public CustomerController(CustomerService customerService, AccessControlUtil accessControl) {
        this.customerService = customerService;
        this.accessControl = accessControl;
    }

    @GetMapping("/all")
    @Operation(summary = "Get All Customers", description = "Super Admin only: Get all customers with pagination, filtering, and sorting")
    public ResponseEntity<Map<String, Object>> getAllCustomers(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            // Only Super Admin can access all customers
            if (!accessControl.isSuperAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, "Access denied. Only Super Admin can access all customers."));
            }
            
            com.cloudkitchen.rbac.dto.common.PageRequest pageRequest = 
                new com.cloudkitchen.rbac.dto.common.PageRequest(page, size, sortBy, sortDirection);
            
            com.cloudkitchen.rbac.dto.common.PageResponse<CustomerResponse> response = 
                customerService.getAllCustomers(pageRequest, status, search);
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, 
                        String.format("Customers retrieved successfully. Page %d of %d, Total: %d", 
                            response.getPage() + 1, response.getTotalPages(), response.getTotalElements()), 
                        response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving customers"));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Customer by ID", description = "Get a specific customer by their ID")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Integer id, Authentication authentication) {
        try {
            boolean hasAccess = accessControl.isSuperAdmin(authentication) || 
                                accessControl.hasPermission(authentication, "customer.read") ||
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
    @Operation(summary = "Get Customers by Merchant", description = "Get merchant's customers with pagination, filtering, and sorting")
    public ResponseEntity<Map<String, Object>> getCustomersByMerchant(
            @PathVariable Integer merchantId, 
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search) {
        try {
            // Super Admin can access any merchant's customers
            if (accessControl.isSuperAdmin(authentication)) {
                com.cloudkitchen.rbac.dto.common.PageRequest pageRequest = 
                    new com.cloudkitchen.rbac.dto.common.PageRequest(page, size, sortBy, sortDirection);
                
                com.cloudkitchen.rbac.dto.common.PageResponse<CustomerResponse> response = 
                    customerService.getCustomersByMerchantId(merchantId, pageRequest);
                
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ResponseBuilder.success(200, 
                            String.format("Customers for merchant ID %d retrieved successfully. Page %d of %d, Total: %d", 
                                merchantId, response.getPage() + 1, response.getTotalPages(), response.getTotalElements()), 
                            response));
            }
            
            // Merchant can only access their own customers
            if (!customerService.canAccessMerchantCustomers(authentication, merchantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, "Access denied. You can only access your own merchant's customers."));
            }
            
            com.cloudkitchen.rbac.dto.common.PageRequest pageRequest = 
                new com.cloudkitchen.rbac.dto.common.PageRequest(page, size, sortBy, sortDirection);
            
            com.cloudkitchen.rbac.dto.common.PageResponse<CustomerResponse> response = 
                customerService.getCustomersByMerchantId(merchantId, pageRequest);
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, 
                        String.format("Customers for merchant ID %d retrieved successfully. Page %d of %d, Total: %d", 
                            merchantId, response.getPage() + 1, response.getTotalPages(), response.getTotalElements()), 
                        response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Merchant not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "Merchant not found with ID: " + merchantId));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving customers for merchant"));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Customer", description = "Super Admin only: Delete a customer by ID")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable Integer id, Authentication authentication) {
        try {
            // Only Super Admin can delete customers
            if (!accessControl.isSuperAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, "Access denied. Only Super Admin can delete customers."));
            }
            
            customerService.deleteCustomer(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Customer deleted successfully"));
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
                    .body(ResponseBuilder.error(500, "Internal server error while deleting customer"));
        }
    }
}