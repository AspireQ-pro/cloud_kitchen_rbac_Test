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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
        summary = "Create Merchant",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Authentication:** Super Admin or merchants.create permission required\n" +
                     "2. **Authorization Header:** Bearer {your_jwt_token}\n" +
                     "3. **Request Body:** JSON with merchant details and user account info\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid merchant creation with all fields\n" +
                     "- Missing required fields (400)\n" +
                     "- Duplicate merchant email (409)\n" +
                     "- Duplicate merchant phone (409)\n" +
                     "- Invalid email format (400)\n" +
                     "- Insufficient permissions (403)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Merchant created successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":201,\"message\":\"Merchant 'ABC Restaurant' created successfully with ID: 1\",\"data\":{\"merchantId\":1,\"merchantName\":\"ABC Restaurant\",\"email\":\"admin@abc.com\"}}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "409", description = "Merchant already exists")
    })
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.create')")
    public ResponseEntity<Map<String, Object>> createMerchant(@Valid @RequestBody MerchantRequest request) {
        try {
            MerchantResponse response = merchantService.createMerchant(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBuilder.success(201, "Merchant '" + request.getMerchantName() + "' created successfully with ID: " + response.getMerchantId(), response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(409, e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, "Failed to create merchant: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Update Merchant",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Authentication:** Super Admin or own merchant access required\n" +
                     "2. **Authorization Header:** Bearer {your_jwt_token}\n" +
                     "3. **Path Parameter:** Valid merchant ID\n" +
                     "4. **Request Body:** JSON with fields to update\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid update with partial fields\n" +
                     "- Update own merchant data\n" +
                     "- Access other merchant's data (403)\n" +
                     "- Invalid merchant ID (404)\n" +
                     "- Invalid field values (400)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Merchant updated successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Merchant ID 1 updated successfully\",\"data\":{\"merchantId\":1,\"merchantName\":\"Updated Restaurant\"}}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Merchant not found")
    })
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
    @Operation(
        summary = "Get Merchant by ID",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Authentication:** Super Admin, merchants.read permission, or own merchant access\n" +
                     "2. **Authorization Header:** Bearer {your_jwt_token}\n" +
                     "3. **Path Parameter:** Valid merchant ID\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid merchant ID (own data)\n" +
                     "- Valid merchant ID (admin access)\n" +
                     "- Access other merchant's data (403)\n" +
                     "- Invalid merchant ID (404)\n" +
                     "- No authentication (401)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Merchant retrieved successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Merchant profile retrieved successfully\",\"data\":{\"merchantId\":1,\"merchantName\":\"ABC Restaurant\",\"email\":\"admin@abc.com\"}}"))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Merchant not found")
    })
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
    @Operation(
        summary = "Get All Merchants",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Authentication:** Super Admin or merchants.read permission required\n" +
                     "2. **Authorization Header:** Bearer {your_jwt_token}\n" +
                     "3. **Query Parameters:** page, size, sortBy, sortDirection, status, search (all optional)\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Default pagination (page=0, size=20)\n" +
                     "- Custom pagination and sorting\n" +
                     "- Filter by status (active/inactive)\n" +
                     "- Search by merchant name\n" +
                     "- Combined filters and search\n" +
                     "- Insufficient permissions (403)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Merchants retrieved successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Merchants retrieved successfully. Page 1 of 5, Total: 100\",\"data\":{\"content\":[{\"merchantId\":1,\"merchantName\":\"ABC Restaurant\"}],\"page\":0,\"size\":20}}"))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
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
    @Operation(
        summary = "Delete Merchant",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Authentication:** Super Admin only\n" +
                     "2. **Authorization Header:** Bearer {your_jwt_token}\n" +
                     "3. **Path Parameter:** Valid merchant ID\n" +
                     "4. **Warning:** This action is irreversible\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid merchant ID deletion\n" +
                     "- Invalid merchant ID (404)\n" +
                     "- Merchant with dependencies (409)\n" +
                     "- Non-super-admin access (403)\n" +
                     "- No authentication (401)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Merchant deleted successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Merchant ID 1 deleted successfully\"}"))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Super Admin access required"),
        @ApiResponse(responseCode = "404", description = "Merchant not found"),
        @ApiResponse(responseCode = "409", description = "Conflict - Merchant cannot be deleted")
    })
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