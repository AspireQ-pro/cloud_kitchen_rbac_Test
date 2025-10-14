package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.service.S3FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final S3FolderService s3FolderService;

    public FolderController(S3FolderService s3FolderService) {
        this.s3FolderService = s3FolderService;
    }

    @PostMapping("/merchant/{merchantId}")
    public ResponseEntity<Map<String, String>> createMerchantFolders(@PathVariable String merchantId) {
        s3FolderService.createMerchantFolderStructure(merchantId);
        return ResponseEntity.ok(Map.of("message", "Merchant folders created successfully", "merchantId", merchantId));
    }

    @PostMapping("/customer/{merchantId}/{customerId}")
    public ResponseEntity<Map<String, String>> createCustomerFolders(
            @PathVariable String merchantId, 
            @PathVariable String customerId) {
        s3FolderService.createCustomerFolderStructure(merchantId, customerId);
        return ResponseEntity.ok(Map.of("message", "Customer folders created successfully", 
                                      "merchantId", merchantId, "customerId", customerId));
    }
}