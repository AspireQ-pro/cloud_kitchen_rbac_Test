package com.cloudkitchen.rbac.controller;
// Java Imports
import java.util.Map;
// Spring Framework Imports
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
// Spring Framework Annotations
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
// Project Imports
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.service.MerchantService;
// Jakarta Validation Import
import jakarta.validation.Valid;

/**
 * HTTP adapter for merchant endpoints. This controller delegates all business,
 * authorization, and response formatting to the service layer.
 *
 * Related files and responsibilities:
 * - Service contract: {@code com.cloudkitchen.rbac.service.MerchantService}
 * - Service implementation (logic/validation/response building):
 *   {@code com.cloudkitchen.rbac.service.impl.MerchantServiceImpl}
 * - DTOs: {@code com.cloudkitchen.rbac.dto.merchant.MerchantRequest},
 *   {@code com.cloudkitchen.rbac.dto.merchant.MerchantResponse}
 * - Validation rules: DTO annotations + {@code com.cloudkitchen.rbac.service.ValidationService}
 * - Access control helpers: {@code com.cloudkitchen.rbac.util.AccessControlUtil}
 * - Error formatting: exception handlers in {@code com.cloudkitchen.rbac.exception}
 * - Response format helper: {@code com.cloudkitchen.rbac.util.ResponseBuilder}
 * - Persistence: {@code com.cloudkitchen.rbac.repository.MerchantRepository},
 *   {@code com.cloudkitchen.rbac.repository.UserRepository}
 * - API docs: {@code src/main/resources/openapi/merchants.yaml} and
 *   {@code src/main/resources/openapi/openapi.yaml}
 *
 * Endpoint flow: HTTP -> Controller -> MerchantService -> ResponseEntity.
 */
@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * Construct the controller with the merchant service dependency.
     */
    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * Create a merchant and associated merchant user account.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMerchant(@Valid @RequestBody MerchantRequest request,
            Authentication authentication) {
        return merchantService.createMerchantResponse(request, authentication);
    }

    /**
     * Update merchant fields for the specified merchant ID.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMerchant(@PathVariable Integer id,
            @RequestBody MerchantRequest request,
            Authentication authentication) {
        return merchantService.updateMerchantResponse(id, request, authentication);
    }

    /**
     * Fetch a merchant profile by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable Integer id, Authentication authentication) {
        return merchantService.getMerchantResponse(id, authentication);
    }

    /**
     * List merchants with pagination, sorting, and optional filters.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        return merchantService.getAllMerchantsResponse(page, size, sortBy, sortDirection, status, search, authentication);
    }

    /**
     * Delete a merchant and related user records by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMerchant(@PathVariable Integer id,
            Authentication authentication) {
        return merchantService.deleteMerchantResponse(id, authentication);
    }
}
