package com.cloudkitchen.rbac.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;
import com.cloudkitchen.rbac.service.MerchantService;
import com.cloudkitchen.rbac.util.AccessControlUtil;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/merchants")
@Tag(name = "Merchant Management", description = "Merchant CRUD operations")
public class MerchantController {
    private static final String NOT_FOUND = "not found";
    private static final String MERCHANT_NOT_FOUND_MSG = "Merchant not found with ID: ";
    
    private final MerchantService merchantService;
    private final AccessControlUtil accessControl;

    public MerchantController(MerchantService merchantService, AccessControlUtil accessControl) {
        this.merchantService = merchantService;
        this.accessControl = accessControl;
    }

    @PostMapping
    @Operation(summary = "Create Merchant", description = "Create a new merchant with user account")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.create')")
    public ResponseEntity<Map<String, Object>> createMerchant(@Valid @RequestBody MerchantRequest request) {
        MerchantResponse response = merchantService.createMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Merchant '" + request.getMerchantName() + "' created successfully with ID: " + response.getMerchantId(), response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update Merchant", description = "Full update of merchant details")
    public ResponseEntity<Map<String, Object>> updateMerchant(@PathVariable Integer id, @RequestBody MerchantRequest request, Authentication authentication) {
        if (!accessControl.isSuperAdmin(authentication) && !merchantService.canAccessMerchant(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(403, "Access denied: You can only update your own merchant data"));
        }

        MerchantResponse response = merchantService.updateMerchant(id, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseBuilder.success(200, "Merchant ID " + id + " updated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Merchant", description = "Get merchant by ID")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable Integer id, Authentication authentication) {
        try {
            if (!accessControl.isSuperAdmin(authentication) && 
                !accessControl.hasPermission(authentication, "merchants.read") && 
                !merchantService.canAccessMerchant(authentication, id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, "Access denied: You can only view your own merchant data"));
            }
            
            MerchantResponse response = merchantService.getMerchantById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Merchant profile retrieved successfully", response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains(NOT_FOUND)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, MERCHANT_NOT_FOUND_MSG + id));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving merchant"));
        }
    }

    @GetMapping
    @Operation(summary = "Get All Merchants", description = "Get all merchants with pagination, filtering, and sorting")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.read')")
    public ResponseEntity<Map<String, Object>> getAllMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            com.cloudkitchen.rbac.dto.common.PageRequest pageRequest = 
                new com.cloudkitchen.rbac.dto.common.PageRequest(page, size, sortBy, sortDirection);
            
            com.cloudkitchen.rbac.dto.common.PageResponse<MerchantResponse> response = 
                merchantService.getAllMerchants(pageRequest, status, search);
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, 
                        String.format("Merchants retrieved successfully. Page %d of %d, Total: %d", 
                            response.getPage() + 1, response.getTotalPages(), response.getTotalElements()), 
                        response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving merchants"));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Merchant", description = "Delete merchant by ID (Super Admin only)")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteMerchant(@PathVariable Integer id, Authentication authentication) {
        try {
            
            merchantService.deleteMerchant(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Merchant ID " + id + " deleted successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains(NOT_FOUND)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, MERCHANT_NOT_FOUND_MSG + id));
            }
            if (e.getMessage().contains("cannot be deleted")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(409, "Merchant cannot be deleted: " + e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while deleting merchant"));
        }
    }
}