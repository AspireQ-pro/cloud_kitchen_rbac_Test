package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.service.FileUploadService;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "File Upload", description = "File upload operations for S3")
public class FileUploadController {

    private static final String IMAGE_URL = "imageUrl";
    private static final String MERCHANT_ID = "merchantId";
    
    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/offer/{merchantId}")
    @Operation(summary = "Upload Offer Image", description = "Upload offer image to S3 bucket")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.update')")
    public ResponseEntity<Map<String, Object>> uploadOfferImage(
            @PathVariable String merchantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "global", defaultValue = "false") boolean global) {
        
        String imageUrl = fileUploadService.uploadOfferImage(merchantId, file, global);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Offer image uploaded successfully", 
                    Map.of(IMAGE_URL, imageUrl, MERCHANT_ID, merchantId, "global", global)));
    }

    @PostMapping("/banner/{merchantId}")
    @Operation(summary = "Upload Banner Image", description = "Upload banner image to S3 bucket")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.update')")
    public ResponseEntity<Map<String, Object>> uploadBannerImage(
            @PathVariable String merchantId,
            @RequestParam("file") MultipartFile file) {
        
        String imageUrl = fileUploadService.uploadBannerImage(merchantId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Banner image uploaded successfully", 
                    Map.of(IMAGE_URL, imageUrl, MERCHANT_ID, merchantId)));
    }

    @PostMapping("/product/{merchantId}")
    @Operation(summary = "Upload Product Image", description = "Upload product image to S3 bucket")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.update')")
    public ResponseEntity<Map<String, Object>> uploadProductImage(
            @PathVariable String merchantId,
            @RequestParam("file") MultipartFile file) {
        
        String imageUrl = fileUploadService.uploadProductImage(merchantId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Product image uploaded successfully", 
                    Map.of(IMAGE_URL, imageUrl, MERCHANT_ID, merchantId)));
    }

    @PostMapping("/menu-card/{merchantId}")
    @Operation(summary = "Upload Menu Card Image", description = "Upload menu card image to S3 bucket") 
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.update')")   
    public ResponseEntity<Map<String, Object>> uploadMenuCardImage(
            @PathVariable String merchantId,
            @RequestParam("file") MultipartFile file) {
        
        String imageUrl = fileUploadService.uploadMenuCardImage(merchantId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Menu card image uploaded successfully", 
                    Map.of(IMAGE_URL, imageUrl, MERCHANT_ID, merchantId)));
    }
    
    @PostMapping("/profile/{merchantId}")
    @Operation(summary = "Upload Profile Image", description = "Upload merchant profile image to S3 bucket")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('merchants.update')")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @PathVariable String merchantId,
            @RequestParam("file") MultipartFile file) {
        
        String imageUrl = fileUploadService.uploadProfileImage(merchantId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "Profile image uploaded successfully", 
                    Map.of(IMAGE_URL, imageUrl, MERCHANT_ID, merchantId)));
    }
}