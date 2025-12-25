package com.cloudkitchen.rbac.service.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.cloudkitchen.rbac.config.S3Properties;
import com.cloudkitchen.rbac.exception.BusinessExceptions.ServiceUnavailableException;
import com.cloudkitchen.rbac.service.CloudStorageService;
import com.cloudkitchen.rbac.util.FilenameSanitizer;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Service
public class S3CloudStorageServiceImpl implements CloudStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3CloudStorageServiceImpl.class);
    private static final Pattern VALID_ID = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final String FOLDER_PLACEHOLDER = "# folder placeholder";
    private static final byte[] PLACEHOLDER_BYTES = FOLDER_PLACEHOLDER.getBytes(StandardCharsets.UTF_8);
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;
    
    public S3CloudStorageServiceImpl(S3Client s3Client, S3Presigner s3Presigner, S3Properties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }
    
    @Override
    public void createMerchantFolderStructure(String merchantId) {
        validateId(merchantId, "merchantId");
        
        // Global folders (root level)
        String[] globalFolders = {"offers/", "ads/"};
        
        // Merchant-specific folders
        String[] merchantFolders = {
            "banners/", "logos/", "profile_image/", "product_image/",
            "menu_card/", "offers/"
        };
        
        long start = System.currentTimeMillis();
        int success = 0;
        int failed = 0;
        
        // Create global folders (only once, idempotent)
        for (String folder : globalFolders) {
            try {
                createFolder(folder);
                success++;
            } catch (Exception e) {
                failed++;
                logger.error("Failed to create global folder {}: {}", folder, e.getMessage());
            }
        }
        
        // Create merchant-specific folders
        for (String folder : merchantFolders) {
            try {
                createFolder(merchantId + "/" + folder);
                success++;
            } catch (Exception e) {
                failed++;
                logger.error("Failed to create folder {}/{}: {}", merchantId, folder, e.getMessage());
            }
        }
        
        if (failed > 0) {
            throw new ServiceUnavailableException(
                String.format("Failed to create %d/%d folders for merchant %s", failed, globalFolders.length + merchantFolders.length, merchantId)
            );
        }
        
        logger.info("✅ Merchant folders created - merchantId: {}, folders: {}, duration: {}ms",
            merchantId, success, System.currentTimeMillis() - start);
    }

    @Async("s3Executor")
    @Override
    public CompletableFuture<Void> createMerchantFolderStructureAsync(String merchantId) {
        try {
            createMerchantFolderStructure(merchantId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.warn("Async S3 folder creation failed for merchant {}: {}", merchantId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void createCustomerFolderStructure(String merchantId, String customerId) {
        validateId(merchantId, "merchantId");
        validateId(customerId, "customerId");
        
        String[] folders = {"profile_img/", "reviews/"};
        
        long start = System.currentTimeMillis();
        int failed = 0;
        
        for (String folder : folders) {
            try {
                createFolder(merchantId + "/customer/" + customerId + "/" + folder);
            } catch (Exception e) {
                failed++;
                logger.error("Failed to create customer folder: {}", e.getMessage());
            }
        }
        
        if (failed > 0) {
            throw new ServiceUnavailableException(
                String.format("Failed to create %d/%d customer folders", failed, folders.length)
            );
        }
        
        logger.info("✅ Customer folders created - merchantId: {}, customerId: {}, folders: {}, duration: {}ms",
            merchantId, customerId, folders.length, System.currentTimeMillis() - start);
    }
    
    @Override
    public void uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        validateKey(key);
        if (inputStream == null) throw new IllegalArgumentException("InputStream cannot be null");
        if (contentLength <= 0) throw new IllegalArgumentException("Content length must be > 0");
        if (contentLength > 10485760) throw new IllegalArgumentException("File size exceeds 10MB limit");
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type is required");
        }
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
            
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            logger.info("✅ File uploaded - key: {}, size: {} bytes", key, contentLength);
            
        } catch (S3Exception e) {
            logger.error("❌ S3 upload failed - key: {}, error: {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new ServiceUnavailableException("S3 upload failed: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            logger.error("❌ Upload failed - key: {}, error: {}", key, e.getMessage(), e);
            throw new ServiceUnavailableException("Upload failed: " + e.getMessage());
        }
    }
    
    private void createFolder(String path) {
        String key = FilenameSanitizer.sanitizePath(path) + ".keep";
        
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType("text/plain")
                .contentLength((long) PLACEHOLDER_BYTES.length)
                .build();
            
            s3Client.putObject(request, RequestBody.fromBytes(PLACEHOLDER_BYTES));
            
        } catch (S3Exception e) {
            logger.error("S3 folder creation failed - path: {}, error: {}", path, e.awsErrorDetails().errorMessage(), e);
            throw new ServiceUnavailableException("Folder creation failed: " + e.awsErrorDetails().errorMessage());
        }
    }
    
    private void validateId(String id, String field) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be null or empty");
        }
        if (!VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException(field + " contains invalid characters (only alphanumeric, -, _ allowed)");
        }
        if (id.length() > 100) {
            throw new IllegalArgumentException(field + " exceeds max length of 100");
        }
    }
    
    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }
        if (key.contains("..")) {
            throw new SecurityException("Path traversal detected in S3 key");
        }
        if (key.contains("//")) {
            throw new IllegalArgumentException("Invalid path: consecutive slashes not allowed");
        }
        if (key.length() > 1024) {
            throw new IllegalArgumentException("S3 key exceeds max length of 1024");
        }
    }
    
    @Override
    public String generatePresignedUrl(String key) {
        validateKey(key);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .getObjectRequest(getObjectRequest)
                .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
            
        } catch (Exception e) {
            logger.error("Failed to generate presigned URL for key: {}", key, e);
            throw new ServiceUnavailableException("Failed to generate presigned URL: " + e.getMessage());
        }
    }
    
}
