package com.cloudkitchen.rbac.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.dto.website.WebsiteConfigRequest;
import com.cloudkitchen.rbac.dto.website.WebsiteConfigResponse;
import com.cloudkitchen.rbac.service.WebsiteConfigService;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchants/{merchantId}/website-config")
@Tag(name = "Website Configuration", description = "Merchant website configuration management")
public class WebsiteConfigController {
    
    private static final Logger log = LoggerFactory.getLogger(WebsiteConfigController.class);
    private final WebsiteConfigService websiteConfigService;

    public WebsiteConfigController(WebsiteConfigService websiteConfigService) {
        this.websiteConfigService = websiteConfigService;
    }

    @PostMapping
    @Operation(summary = "Save website configuration", description = "Create new website configuration for merchant")
    public ResponseEntity<Object> saveConfiguration(
            @PathVariable Integer merchantId,
            @Valid @RequestBody WebsiteConfigRequest request) {
        
        log.info("POST /api/merchants/{}/website-config - Save configuration", merchantId);
        
        WebsiteConfigResponse response = websiteConfigService.saveConfiguration(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseBuilder.success(201, "Website configuration saved successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get website configuration", description = "Retrieve website configuration for merchant")
    public ResponseEntity<Object> getConfiguration(@PathVariable Integer merchantId) {
        
        log.info("GET /api/merchants/{}/website-config - Get configuration", merchantId);
        
        WebsiteConfigResponse response = websiteConfigService.getConfiguration(merchantId);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Configuration retrieved successfully", response));
    }

    @PostMapping("/publish")
    @Operation(summary = "Publish website", description = "Publish merchant website")
    public ResponseEntity<Object> publishWebsite(@PathVariable Integer merchantId) {
        
        log.info("POST /api/merchants/{}/website-config/publish - Publish website", merchantId);
        
        WebsiteConfigResponse response = websiteConfigService.publishWebsite(merchantId);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Website published successfully", response));
    }

    @PatchMapping
    @Operation(summary = "Update website configuration", description = "Partial or full update of website configuration")
    public ResponseEntity<Object> patchConfiguration(
            @PathVariable Integer merchantId,
            @Valid @RequestBody com.cloudkitchen.rbac.dto.website.WebsiteConfigUpdateRequest request) {
        
        log.info("PATCH /api/merchants/{}/website-config - Update configuration", merchantId);
        
        WebsiteConfigResponse response = websiteConfigService.patchConfiguration(merchantId, request);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Website configuration updated successfully", response));
    }
}
