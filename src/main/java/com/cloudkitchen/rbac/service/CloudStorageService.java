package com.cloudkitchen.rbac.service;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * Cloud storage service abstraction for file and folder operations.
 * This interface provides a clean separation between business logic and storage implementation.
 */
public interface CloudStorageService {

    /**
     * Creates the complete folder structure for a merchant in cloud storage.
     *
     * @param merchantId the merchant identifier (must be numeric)
     * @throws IllegalArgumentException if merchantId is invalid
     * @throws com.cloudkitchen.rbac.exception.BusinessExceptions.ServiceUnavailableException if S3 operation fails
     */
    void createMerchantFolderStructure(String merchantId);

    /**
     * Creates the complete folder structure for a merchant in cloud storage asynchronously.
     *
     * @param merchantId the merchant identifier (must be numeric)
     * @return CompletableFuture that completes when folders are created
     */
    CompletableFuture<Void> createMerchantFolderStructureAsync(String merchantId);
    
    /**
     * Creates customer-specific folder structure within a merchant's space.
     * 
     * @param merchantId the merchant identifier (must be numeric)
     * @param customerId the customer identifier (must be numeric)
     * @throws IllegalArgumentException if merchantId or customerId is invalid
     * @throws com.cloudkitchen.rbac.exception.BusinessExceptions.ServiceUnavailableException if S3 operation fails
     */
    void createCustomerFolderStructure(String merchantId, String customerId);
    
    /**
     * Uploads a file to cloud storage.
     * 
     * @param key the S3 object key (path)
     * @param inputStream the file content stream
     * @param contentLength the file size in bytes
     * @param contentType the MIME type of the file
     * @throws IllegalArgumentException if parameters are invalid
     * @throws com.cloudkitchen.rbac.exception.BusinessExceptions.ServiceUnavailableException if S3 operation fails
     */
    void uploadFile(String key, InputStream inputStream, long contentLength, String contentType);
}
