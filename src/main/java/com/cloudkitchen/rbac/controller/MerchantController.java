package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    @Autowired
    private MerchantService merchantService;

    @PostMapping
    public ResponseEntity<Merchant> addMerchant(@RequestBody Merchant merchant) {
        return ResponseEntity.ok(merchantService.addMerchant(merchant));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Merchant> updateMerchant(@PathVariable Integer id, @RequestBody Merchant merchant) {
        return ResponseEntity.ok(merchantService.updateMerchant(id, merchant));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> getMerchantById(@PathVariable Integer id) {
        return ResponseEntity.ok(merchantService.getMerchantById(id));
    }

    @Operation(summary = "Get all merchants")
    @GetMapping
    public ResponseEntity<List<Merchant>> getAllMerchants() {
        return ResponseEntity.ok(merchantService.getAllMerchants());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMerchant(@PathVariable Integer id) {
        merchantService.deleteMerchant(id);
        return ResponseEntity.noContent().build();
    }
}
