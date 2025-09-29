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
import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {
    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createMerchant(@Valid @RequestBody MerchantRequest req) {
        Merchant merchant = merchantService.createMerchant(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Merchant created successfully", merchant));
    }
    
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> createTestMerchant() {
        MerchantRequest req = new MerchantRequest();
        req.setMerchantName("Test Restaurant");
        req.setEmail("test@restaurant.com");
        req.setPhone("9876543210");
        req.setAddress("123 Test Street");
        Merchant merchant = merchantService.createMerchant(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Test merchant created successfully", merchant));
    }
    
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugMerchants() {
        return ResponseEntity.ok(ResponseBuilder.success(200, "Debug info", 
            Map.of("merchantCount", merchantService.getAllMerchants().size(),
                   "merchants", merchantService.getAllMerchants())));
    }
    
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initMerchants() {
        MerchantRequest req1 = new MerchantRequest();
        req1.setMerchantName("Demo Restaurant");
        req1.setEmail("demo@restaurant.com");
        req1.setPhone("9876543210");
        req1.setAddress("123 Main St");
        
        MerchantRequest req2 = new MerchantRequest();
        req2.setMerchantName("Pizza Palace");
        req2.setEmail("info@pizzapalace.com");
        req2.setPhone("9876543211");
        req2.setAddress("456 Oak Ave");
        
        Merchant m1 = merchantService.createMerchant(req1);
        Merchant m2 = merchantService.createMerchant(req2);
        
        return ResponseEntity.ok(ResponseBuilder.success(200, "Merchants created", 
            List.of(m1, m2)));
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