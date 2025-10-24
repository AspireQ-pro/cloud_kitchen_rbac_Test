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
            merchantId + "/profile_image/",
            merchantId + "/product_image/",
            merchantId + "/offers/"
        };

        for (String folder : folders) {
            createFolder(folder);
        }
        
        // Create global offers folder (only once)
        createFolder("offers/");
    }

    public void createCustomerFolderStructure(String merchantId, String customerId) {
        String[] folders = {
            merchantId + "/customer/" + customerId + "/customer_profile_img/",
            merchantId + "/customer/" + customerId + "/review_img/"
        };

        for (String folder : folders) {
            createFolder(folder);
        }
    }
    
    public void createOfferFolderStructure(String merchantId) {
        // This method is now integrated into createMerchantFolderStructure
        // Keeping for backward compatibility if needed
        createFolder(merchantId + "/offers/");
        createFolder("offers/");
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