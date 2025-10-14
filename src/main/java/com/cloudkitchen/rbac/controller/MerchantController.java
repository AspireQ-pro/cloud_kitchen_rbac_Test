package com.cloudkitchen.rbac.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;
import com.cloudkitchen.rbac.service.MerchantService;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchants")
@Tag(name = "Merchant Management", description = "Merchant CRUD operations")
public class MerchantController {
    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    @Operation(summary = "Create Merchant", description = "Create a new merchant with user account")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.create')")
    public ResponseEntity<Map<String, Object>> createMerchant(@Valid @RequestBody MerchantRequest request) {
        try {
            MerchantResponse response = merchantService.createMerchant(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBuilder.success(201, "Merchant '" + request.getMerchantName() + "' created successfully with ID: " + response.getId(), response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(409, "Merchant already exists: " + e.getMessage()));
            }
            if (e.getMessage().contains("validation")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "Validation failed: " + e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while creating merchant"));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Merchant", description = "Update merchant details")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.update')")
    public ResponseEntity<Map<String, Object>> updateMerchant(@PathVariable Integer id, @Valid @RequestBody MerchantRequest request) {
        try {
            MerchantResponse response = merchantService.updateMerchant(id, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Merchant ID " + id + " updated successfully", response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "Merchant not found with ID: " + id));
            }
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(409, "Merchant data conflict: " + e.getMessage()));
            }
            if (e.getMessage().contains("validation")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "Validation failed: " + e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while updating merchant"));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Merchant", description = "Get merchant by ID")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable Integer id, Authentication authentication) {
        try {
            // Check permissions
            boolean hasAccess = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_SUPER_ADMIN".equals(auth.getAuthority()) || 
                                    "merchants.read".equals(auth.getAuthority())) ||
                    merchantService.canAccessMerchant(authentication, id);
            
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, "Access denied"));
            }
            
            MerchantResponse response = merchantService.getMerchantById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Merchant profile retrieved successfully", response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "Merchant not found with ID: " + id));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving merchant"));
        }
    }

    @GetMapping
    @Operation(summary = "Get All Merchants", description = "Get all merchants")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.read')")
    public ResponseEntity<Map<String, Object>> getAllMerchants() {
        try {
            List<MerchantResponse> response = merchantService.getAllMerchants();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "All merchants retrieved successfully. Total: " + response.size(), response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error while retrieving merchants"));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Merchant", description = "Delete merchant by ID")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.delete')")
    public ResponseEntity<Map<String, Object>> deleteMerchant(@PathVariable Integer id) {
        try {
            merchantService.deleteMerchant(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(200, "Merchant ID " + id + " deleted successfully"));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "Merchant not found with ID: " + id));
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