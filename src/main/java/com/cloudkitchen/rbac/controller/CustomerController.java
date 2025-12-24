package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.constants.ResponseMessages;
import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.HttpResponseUtil;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerUpdateRequest request,
            Authentication authentication) {
        if (!customerService.canAccessCustomer(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_PROFILE));
        }
        try {
            Integer updatedBy = authentication != null ? Integer.valueOf(authentication.getName()) : null;
            CustomerResponse response = customerService.updateCustomer(id, request, updatedBy);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.UPDATED_SUCCESS, response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, ResponseMessages.Customer.UPDATE_FAILED + ": " + e.getMessage()));
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
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_LIST));
        }
        try {
            PageRequest pageRequest = new PageRequest(page, size);
            PageResponse<CustomerResponse> customers = customerService.getAllCustomers(pageRequest, status, search, authentication);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.LIST_SUCCESS, customers));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_LIST));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable Integer id, Authentication authentication) {
        if (!customerService.canAccessCustomer(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_VIEW));
        }
        try {
            CustomerResponse customer = customerService.getCustomerById(id);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.RETRIEVED_SUCCESS, customer));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.NOT_FOUND));
        }
    }
    
    @GetMapping("/profile")
    public ResponseEntity<?> getCustomerProfile(Authentication authentication) {
        try {
            CustomerResponse customer = customerService.getCustomerProfile(authentication);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.PROFILE_RETRIEVED_SUCCESS, customer));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.PROFILE_NOT_FOUND));
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
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_MERCHANT_CUSTOMERS));
        }
        try {
            PageRequest pageRequest = new PageRequest(page, size);
            PageResponse<CustomerResponse> customers = customerService.getCustomersByMerchantId(merchantId, pageRequest);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.MERCHANT_CUSTOMERS_SUCCESS, customers));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, ResponseMessages.Customer.RETRIEVE_FAILED + ": " + e.getMessage()));
        }
    }
}