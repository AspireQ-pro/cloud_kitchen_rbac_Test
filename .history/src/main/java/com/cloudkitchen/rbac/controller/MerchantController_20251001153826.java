package com.cloudkitchen.rbac.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('super_admin') or hasPermission('merchant.create')")
    public ResponseEntity<Map<String, Object>> createMerchant(@Valid @RequestBody MerchantRequest request) {
        try {
            MerchantResponse response = merchantService.createMerchant(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBuilder.success(201, "Merchant created successfully", response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(409, e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Failed to create merchant"));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Merchant", description = "Update merchant details")
    @PreAuthorize("hasRole('super_admin') or hasPermission('merchant.update')")
    public ResponseEntity<Map<String, Object>> updateMerchant(@PathVariable Integer id, @Valid @RequestBody MerchantRequest request) {
        try {
            MerchantResponse response = merchantService.updateMerchant(id, request);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Merchant updated successfully", response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, e.getMessage()));
            }
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(409, e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Failed to update merchant"));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Merchant", description = "Get merchant by ID")
    @PreAuthorize("hasRole('super_admin') or hasPermission('merchant.read') or @securityService.canAccessMerchant(authentication.name, #id)")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable Integer id) {
        try {
            MerchantResponse response = merchantService.getMerchantById(id);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Merchant retrieved successfully", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(404, "Merchant not found"));
        }
    }

    @GetMapping
    @Operation(summary = "Get All Merchants", description = "Get all merchants")
    @PreAuthorize("hasRole('super_admin') or hasPermission('merchant.read')")
    public ResponseEntity<Map<String, Object>> getAllMerchants() {
        try {
            List<MerchantResponse> response = merchantService.getAllMerchants();
            return ResponseEntity.ok(ResponseBuilder.success(200, "Merchants retrieved successfully", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Failed to retrieve merchants"));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Merchant", description = "Delete merchant by ID")
    @PreAuthorize("hasRole('super_admin') or hasPermission('merchant.delete')")
    public ResponseEntity<Map<String, Object>> deleteMerchant(@PathVariable Integer id) {
        try {
            merchantService.deleteMerchant(id);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Merchant deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(404, "Merchant not found"));
        }
    }
}