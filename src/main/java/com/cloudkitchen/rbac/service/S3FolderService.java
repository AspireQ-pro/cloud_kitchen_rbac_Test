package com.cloudkitchen.rbac.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

@Service
public class S3FolderService {

    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3FolderService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void createMerchantFolderStructure(String merchantId) {
        String[] folders = {
            merchantId + "/banner/",
            merchantId + "/profileimg/",
            merchantId + "/productimg/",
            merchantId + "/customers/"
        };
        
        for (String folder : folders) {
            createFolder(folder);
        }
    }

    public void createCustomerFolderStructure(String merchantId, String customerId) {
        String[] folders = {
            merchantId + "/customers/" + customerId + "/profileimg/",
            merchantId + "/customers/" + customerId + "/reviews/"
        };
        
        for (String folder : folders) {
            createFolder(folder);
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
            // Log error but don't throw exception to prevent blocking operations
            System.err.println("Warning: Failed to create S3 folder " + folderPath + ": " + e.getMessage());
        }
    }
}