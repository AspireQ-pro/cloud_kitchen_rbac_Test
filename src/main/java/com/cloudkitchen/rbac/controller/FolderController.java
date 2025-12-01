package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.service.CloudStorageService;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/folders")
@Tag(name = "Folder Management", description = "S3 folder structure operations")
public class FolderController {
    
    private final CloudStorageService cloudStorageService;
    
    public FolderController(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
    }
    
    @PostMapping("/merchant/{merchantId}")
    @Operation(summary = "Create Merchant Folder Structure")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.write')")
    public ResponseEntity<Map<String, Object>> createMerchantFolders(@PathVariable String merchantId) {
        try {
            cloudStorageService.createMerchantFolderStructure(merchantId);
            return ResponseEntity.ok(
                ResponseBuilder.success(200, "Merchant folders created successfully",
                    Map.of("merchantId", merchantId))
            );
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseBuilder.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBuilder.error(500, "Failed to create merchant folders: " + e.getMessage()));
        }
    }
    
    @PostMapping("/customer/{merchantId}/{customerId}")
    @Operation(summary = "Create Customer Folder Structure")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.write')")
    public ResponseEntity<Map<String, Object>> createCustomerFolders(
            @PathVariable String merchantId,
            @PathVariable String customerId) {
        try {
            cloudStorageService.createCustomerFolderStructure(merchantId, customerId);
            return ResponseEntity.ok(
                ResponseBuilder.success(200, "Customer folders created successfully",
                    Map.of("merchantId", merchantId, "customerId", customerId))
            );
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseBuilder.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBuilder.error(500, "Failed to create customer folders: " + e.getMessage()));
        }
    }
}
