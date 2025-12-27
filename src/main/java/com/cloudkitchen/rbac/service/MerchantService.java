package com.cloudkitchen.rbac.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;

/**
 * Merchant service contract for merchant lifecycle and response building.
 */
public interface MerchantService {
    /**
     * Create a merchant and its associated merchant user account.
     */
    MerchantResponse createMerchant(MerchantRequest request);
    /**
     * Update merchant fields for the given ID.
     */
    MerchantResponse updateMerchant(Integer id, MerchantRequest request);
    /**
     * Fetch a merchant by ID.
     */
    MerchantResponse getMerchantById(Integer id);
    /**
     * List all merchants without pagination.
     */
    List<MerchantResponse> getAllMerchants();
    /**
     * List merchants with pagination.
     */
    PageResponse<MerchantResponse> getAllMerchants(PageRequest pageRequest);
    /**
     * List merchants with pagination and optional filters.
     */
    PageResponse<MerchantResponse> getAllMerchants(PageRequest pageRequest, String status, String search);
    /**
     * Delete a merchant and its related users.
     */
    void deleteMerchant(Integer id);
    /**
     * Check if the authenticated merchant can access the given merchant ID.
     */
    boolean canAccessMerchant(Authentication authentication, Integer merchantId);

    /**
     * Build the HTTP response for merchant creation.
     */
    ResponseEntity<Map<String, Object>> createMerchantResponse(MerchantRequest request, Authentication authentication);
    /**
     * Build the HTTP response for merchant updates.
     */
    ResponseEntity<Map<String, Object>> updateMerchantResponse(Integer id, MerchantRequest request, Authentication authentication);
    /**
     * Build the HTTP response for fetching a merchant by ID.
     */
    ResponseEntity<Map<String, Object>> getMerchantResponse(Integer id, Authentication authentication);
    /**
     * Build the HTTP response for listing merchants.
     */
    ResponseEntity<Map<String, Object>> getAllMerchantsResponse(int page, int size, String sortBy, String sortDirection, String status, String search, Authentication authentication);
    /**
     * Build the HTTP response for merchant deletion.
     */
    ResponseEntity<Map<String, Object>> deleteMerchantResponse(Integer id, Authentication authentication);
}
