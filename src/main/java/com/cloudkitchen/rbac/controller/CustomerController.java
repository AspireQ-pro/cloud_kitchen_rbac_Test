package com.cloudkitchen.rbac.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.service.CustomerService;

import jakarta.validation.Valid;

/**
 * HTTP adapter for customer endpoints. Delegates authorization, business logic,
 * and response formatting to {@code CustomerService}.
 *
 * Related files: {@code com.cloudkitchen.rbac.service.CustomerService},
 * {@code com.cloudkitchen.rbac.service.impl.CustomerServiceImpl},
 * {@code com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest},
 * {@code com.cloudkitchen.rbac.dto.customer.CustomerResponse},
 * exception handlers in {@code com.cloudkitchen.rbac.exception},
 * {@code com.cloudkitchen.rbac.util.AccessControlUtil},
 * {@code com.cloudkitchen.rbac.util.ResponseBuilder},
 * {@code src/main/resources/openapi/customers.yaml}.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    
    /**
     * Construct the controller with the customer service dependency.
     */
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }


    /**
     * Updates a customer using multipart form-data. The optional "data" part
     * contains a JSON payload (validated) and the optional "profileImage" part
     * contains an image file.
     *
     * @param id customer ID to update
     * @param request optional JSON payload from the "data" part
     * @param profileImage optional profile image file
     * @param authentication current user authentication context
     * @return response with updated customer payload or an error
     */
    @PatchMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<Object> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestPart(value = "data", required = false) CustomerUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            Authentication authentication) {
        return customerService.updateCustomerResponse(id, request, profileImage, authentication);
    }

    /**
     * List customers with pagination, optional status filter, and search.
     */
    @GetMapping
    public ResponseEntity<Object> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        return customerService.getAllCustomersResponse(page, size, status, search, authentication);
    }

    /**
     * Fetch a customer profile by customer ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> getCustomerById(
            @PathVariable Integer id,
            Authentication authentication) {
        return customerService.getCustomerByIdResponse(id, authentication);
    }
    
    /**
     * Fetch the current authenticated customer's profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<Object> getCustomerProfile(Authentication authentication) {
        return customerService.getCustomerProfileResponse(authentication);
    }
    
    /**
     * List customers for a given merchant with pagination.
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<Object> getCustomersByMerchantId(
            @PathVariable Integer merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return customerService.getCustomersByMerchantIdResponse(merchantId, page, size, authentication);
    }

    /**
     * Soft-delete a customer by customer ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteCustomer(
            @PathVariable Integer id,
            Authentication authentication) {
        return customerService.deleteCustomerResponse(id, authentication);
    }
}
