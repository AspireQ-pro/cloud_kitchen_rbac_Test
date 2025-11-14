package com.cloudkitchen.rbac.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class CloudStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CloudStorageService.class);
    
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public CloudStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void createMerchantFolderStructure(String merchantId) {
        String[] folders = {
            merchantId + "/banners/",
            merchantId + "/logo/",
            merchantId + "/profile_image/",
            merchantId + "/product_image/",
            merchantId + "/menu_card/",
            merchantId + "/offers/",
            merchantId + "/documents/",
            merchantId + "/website/",
            merchantId + "/website/static/css/",
            merchantId + "/website/static/js/",
            merchantId + "/website/static/media/"
        };

        for (String folder : folders) {
            createFolder(folder);
        }
        
        logger.info("Created S3 folder structure for merchant: {}", merchantId);
    }

    public void createCustomerFolderStructure(String merchantId, String customerId) {
        String[] folders = {
            merchantId + "/customer/" + customerId + "/profile_img/",
            merchantId + "/customer/" + customerId + "/reviews/"
        };

        for (String folder : folders) {
            createFolder(folder);
        }
        
        logger.info("Created S3 customer folders for merchant: {}, customer: {}", merchantId, customerId);
    }
    
    public void uploadFile(String key, java.io.InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            logger.info("Uploaded file to S3: {}", key);
        } catch (Exception e) {
            logger.error("Failed to upload file to S3: {}", key, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    private void createFolder(String folderPath) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(folderPath + ".keep")
                .contentType("text/plain")
                .build();

            s3Client.putObject(request, RequestBody.fromString("# Folder placeholder"));
        } catch (Exception e) {
            logger.warn("Failed to create S3 folder {}: {}", folderPath, e.getMessage());
        }
    }
}