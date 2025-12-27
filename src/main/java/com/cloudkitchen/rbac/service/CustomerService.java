package com.cloudkitchen.rbac.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;

/**
 * Customer service contract for customer access, updates, and response building.
 */
public interface CustomerService {
    // Read operations
    /**
     * List all active customers without access checks.
     */
    List<CustomerResponse> getAllCustomers();
    /**
     * List customers visible to the authenticated user.
     */
    List<CustomerResponse> getAllCustomers(Authentication authentication);
    /**
     * List customers with pagination.
     */
    PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest);
    /**
     * List customers with pagination and optional status/search filters.
     */
    PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search);
    /**
     * List customers with access checks and optional status/search filters.
     */
    PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search, Authentication authentication);
    /**
     * List customers for a merchant.
     */
    List<CustomerResponse> getCustomersByMerchantId(Integer merchantId);
    /**
     * List customers for a merchant with pagination.
     */
    PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, PageRequest pageRequest);
    /**
     * Fetch a customer by ID.
     */
    CustomerResponse getCustomerById(Integer id);
    /**
     * Fetch a customer by ID with access checks.
     */
    CustomerResponse getCustomerById(Integer id, Authentication authentication);
    /**
     * Fetch the authenticated customer's profile.
     */
    CustomerResponse getCustomerProfile(Authentication authentication);
    
    // Update operations
    /**
     * Update customer fields and set updatedBy.
     */
    CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, Integer updatedBy);
    /**
     * Update customer fields and optional profile image.
     */
    CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Integer updatedBy);
    /**
     * Update customer using authentication for updatedBy.
     */
    CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Authentication authentication);
    /**
     * Update the authenticated customer's profile.
     */
    CustomerResponse updateCustomerProfile(Authentication authentication, CustomerUpdateRequest request);

    // List operations with pagination
    /**
     * List customers using primitive pagination parameters and access checks.
     */
    PageResponse<CustomerResponse> getAllCustomers(int page, int size, String status, String search, Authentication authentication);
    /**
     * List customers for a merchant using primitive pagination parameters.
     */
    PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, int page, int size, Authentication authentication);
    
    // Delete operations
    /**
     * Soft-delete a customer by ID.
     */
    void deleteCustomer(Integer id);
    
    // Access control methods
    /**
     * Check if the authenticated user can list customers.
     */
    boolean canAccessCustomers(Authentication authentication);
    /**
     * Check if the authenticated user can access a customer by ID.
     */
    boolean canAccessCustomer(Authentication authentication, Integer customerId);
    /**
     * Check if the authenticated user can access a merchant's customers.
     */
    boolean canAccessMerchantCustomers(Authentication authentication, Integer merchantId);
    /**
     * Extract merchant ID from authentication details.
     */
    Integer getMerchantIdFromAuth(Authentication authentication);
    /**
     * Extract customer user ID from authentication details.
     */
    Integer getCustomerIdFromAuth(Authentication authentication);

    // Controller response methods
    /**
     * Build the HTTP response for updating a customer.
     */
    ResponseEntity<Object> updateCustomerResponse(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Authentication authentication);
    /**
     * Build the HTTP response for listing customers.
     */
    ResponseEntity<Object> getAllCustomersResponse(int page, int size, String status, String search, Authentication authentication);
    /**
     * Build the HTTP response for fetching a customer by ID.
     */
    ResponseEntity<Object> getCustomerByIdResponse(Integer id, Authentication authentication);
    /**
     * Build the HTTP response for fetching the authenticated customer's profile.
     */
    ResponseEntity<Object> getCustomerProfileResponse(Authentication authentication);
    /**
     * Build the HTTP response for listing customers by merchant ID.
     */
    ResponseEntity<Object> getCustomersByMerchantIdResponse(Integer merchantId, int page, int size, Authentication authentication);
    /**
     * Build the HTTP response for deleting a customer.
     */
    ResponseEntity<Object> deleteCustomerResponse(Integer id, Authentication authentication);
}
