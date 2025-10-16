package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.service.CloudStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final CloudStorageService cloudStorageService;

    public FolderController(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
    }

    @PostMapping("/merchant/{merchantId}")
    public ResponseEntity<Map<String, String>> createMerchantFolders(@PathVariable String merchantId) {
        cloudStorageService.createMerchantFolderStructure(merchantId);
        return ResponseEntity.ok(Map.of("message", "Merchant folders created successfully", "merchantId", merchantId));
    }

    @PostMapping("/customer/{merchantId}/{customerId}")
    public ResponseEntity<Map<String, String>> createCustomerFolders(
            @PathVariable String merchantId,
            @PathVariable String customerId) {
        cloudStorageService.createCustomerFolderStructure(merchantId, customerId);
        return ResponseEntity.ok(Map.of("message", "Customer folders created successfully",
                                      "merchantId", merchantId, "customerId", customerId));
    }
}