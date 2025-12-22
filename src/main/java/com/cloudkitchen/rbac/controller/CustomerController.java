package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerUpdateRequest request,
            Authentication authentication) {
        Integer updatedBy = authentication != null ? Integer.valueOf(authentication.getName()) : null;
        CustomerResponse response = customerService.updateCustomer(id, request, updatedBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        PageRequest pageRequest = new PageRequest(page, size);
        PageResponse<CustomerResponse> customers = customerService.getAllCustomers(pageRequest, status, search, authentication);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Integer id) {
        CustomerResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }
    
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<PageResponse<CustomerResponse>> getCustomersByMerchantId(
            @PathVariable Integer merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        PageRequest pageRequest = new PageRequest(page, size);
        PageResponse<CustomerResponse> customers = customerService.getCustomersByMerchantId(merchantId, pageRequest);
        return ResponseEntity.ok(customers);
    }
}