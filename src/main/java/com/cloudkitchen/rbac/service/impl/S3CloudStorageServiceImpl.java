package com.cloudkitchen.rbac.service.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudkitchen.rbac.config.S3Properties;
import com.cloudkitchen.rbac.exception.BusinessExceptions.ServiceUnavailableException;
import com.cloudkitchen.rbac.service.CloudStorageService;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3CloudStorageServiceImpl implements CloudStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(S3CloudStorageServiceImpl.class);
    private static final Pattern VALID_ID = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final String FOLDER_PLACEHOLDER = "# folder placeholder";
    private static final byte[] PLACEHOLDER_BYTES = FOLDER_PLACEHOLDER.getBytes(StandardCharsets.UTF_8);
    
    private final S3Client s3Client;
    private final S3Properties properties;
    
    public S3CloudStorageServiceImpl(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }
    
    @Override
    public void createMerchantFolderStructure(String merchantId) {
        validateId(merchantId, "merchantId");
        
        String[] folders = {
            "banners/", "logos/", "profile_image/", "product_image/",
            "menu_card/", "offers/", "website/"
        };
        
        long start = System.currentTimeMillis();
        int success = 0;
        int failed = 0;
        
        for (String folder : folders) {
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
                String.format("Failed to create %d/%d folders for merchant %s", failed, folders.length, merchantId)
            );
        }
        
        logger.info("✅ Merchant folders created - merchantId: {}, folders: {}, duration: {}ms",
            merchantId, success, System.currentTimeMillis() - start);
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
        String key = sanitizePath(path) + ".keep";
        
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
    
    private String sanitizePath(String path) {
        if (path.contains("..")) {
            throw new SecurityException("Path traversal detected");
        }
        return path.replaceAll("[^a-zA-Z0-9/_.-]", "");
    }
}
