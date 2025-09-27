package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.service.MerchantService;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {
    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PreAuthorize("hasRole('super_admin')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMerchant(@Valid @RequestBody MerchantRequest req) {
        Merchant merchant = merchantService.createMerchant(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Merchant created successfully", merchant));
    }

    @PreAuthorize("hasRole('super_admin') or (hasRole('merchant_admin') and @securityService.canAccessMerchant(authentication.name, #id))")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMerchant(@PathVariable Integer id, @Valid @RequestBody MerchantRequest req) {
        Merchant merchant = merchantService.updateMerchant(id, req);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Merchant updated successfully", merchant));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable Integer id) {
        Merchant merchant = merchantService.getMerchantById(id);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Merchant retrieved successfully", merchant));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMerchants() {
        return ResponseEntity.ok(ResponseBuilder.success(200, "Merchants retrieved successfully", merchantService.getAllMerchants()));
    }

    @PreAuthorize("hasRole('super_admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMerchant(@PathVariable Integer id) {
        merchantService.deleteMerchant(id);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Merchant deleted successfully"));
    }
}