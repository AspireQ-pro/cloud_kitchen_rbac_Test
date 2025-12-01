package com.cloudkitchen.rbac.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.cloudkitchen.rbac.config.S3Properties;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudkitchen.rbac.constants.AppConstants;
import com.cloudkitchen.rbac.domain.entity.FileDocument;
import com.cloudkitchen.rbac.repository.FileDocumentRepository;
import com.cloudkitchen.rbac.service.FileService;
import com.cloudkitchen.rbac.util.FilenameSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class FileStorageServiceImpl implements FileService {

    private static final long MULTIPART_THRESHOLD = AppConstants.S3Performance.MULTIPART_THRESHOLD;
    private static final long PRESIGNED_URL_TTL = AppConstants.S3Performance.PRESIGNED_URL_TTL;
    private static final String PROFILE_IMG_FOLDER = "profile_img";
    private static final String REVIEWS_FOLDER = "reviews";
    private static final String MERCHANT_DEFAULT = "merchant_default";

    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;
    private final FileDocumentRepository fileDocumentRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    private final S3Properties s3Properties;

    // Cache for presigned URLs to improve performance
    private final Map<String, String> presignedUrlCache = new ConcurrentHashMap<>();
    private final Map<String, Long> presignedUrlExpiry = new ConcurrentHashMap<>();

    public FileStorageServiceImpl(S3AsyncClient s3AsyncClient,
            S3Presigner s3Presigner, FileDocumentRepository fileDocumentRepository,
            ObjectMapper objectMapper, ApplicationContext applicationContext,
            S3Properties s3Properties) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3Presigner = s3Presigner;
        this.fileDocumentRepository = fileDocumentRepository;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
        this.s3Properties = s3Properties;
    }

    @Override
    public FileDocument uploadFile(String entityType, String entityId, MultipartFile file,
            String documentType, String uploadedByService, Integer uploadedByUserId,
            Map<String, Object> tags) {

        String s3Key = generateS3Key(entityType, entityId, documentType, file.getOriginalFilename());

        // Save metadata first for immediate response
        FileDocument document = createFileDocument(entityType, entityId, uploadedByService,
                documentType, s3Key, file, tags, uploadedByUserId);
        document = fileDocumentRepository.save(document);

        // Upload asynchronously using proxy to enable @Async
        applicationContext.getBean(FileStorageServiceImpl.class).uploadFileAsync(file, s3Key);

        return document;
    }

    @Async
    public CompletableFuture<Void> uploadFileAsync(MultipartFile file, String s3Key) {
        try {
            if (file.getSize() > MULTIPART_THRESHOLD) {
                return uploadMultipart(file, s3Key);
            } else {
                return uploadSingle(file, s3Key);
            }
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> uploadSingle(MultipartFile file, String s3Key) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            return s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(file.getBytes()))
                    .thenApply(response -> null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> uploadMultipart(MultipartFile file, String s3Key) {
        try {
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            return s3AsyncClient.createMultipartUpload(createRequest)
                    .thenCompose(createResponse -> uploadSingle(file, s3Key));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public List<FileDocument> getFiles(String entityType, String entityId, Map<String, Object> filters) {
        if (filters.containsKey("documentType")) {
            return fileDocumentRepository.findByEntityTypeAndEntityIdAndDocumentType(
                    entityType, entityId, (String) filters.get("documentType"));
        }
        if (filters.containsKey("service")) {
            return fileDocumentRepository.findByEntityAndService(
                    entityType, entityId, (String) filters.get("service"));
        }
        return fileDocumentRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public String generatePresignedUrl(String s3Key, String operation) {
        String cacheKey = s3Key + "_" + operation;
        long currentTime = System.currentTimeMillis();

        // Check cache first
        String cachedUrl = presignedUrlCache.get(cacheKey);
        Long expiryTime = presignedUrlExpiry.get(cacheKey);

        if (cachedUrl != null && expiryTime != null && currentTime < expiryTime) {
            return cachedUrl;
        }

        // Generate new presigned URL
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(req -> req.bucket(s3Properties.getBucket()).key(s3Key))
                .build();

        String newUrl = s3Presigner.presignGetObject(presignRequest).url().toString();

        // Cache the new URL
        presignedUrlCache.put(cacheKey, newUrl);
        presignedUrlExpiry.put(cacheKey, currentTime + PRESIGNED_URL_TTL);

        return newUrl;
    }

    @Override
    public void deleteFile(String docId) {
        // Get file document to retrieve S3 key before deletion
        FileDocument document = fileDocumentRepository.findById(Integer.valueOf(docId))
                .orElse(null);
        
        // Delete from database
        fileDocumentRepository.deleteById(Integer.valueOf(docId));
        
        // Delete from S3 if document exists
        if (document != null && document.getS3Key() != null) {
            try {
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest = 
                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                        .bucket(s3Properties.getBucket())
                        .key(document.getS3Key())
                        .build();
                
                s3AsyncClient.deleteObject(deleteRequest);
            } catch (Exception e) {
                // Log error but don't fail - file may already be deleted
                // Database record is already deleted
            }
        }
    }

    public S3Properties getS3Properties() {
        return s3Properties;
    }

    public java.net.URL generatePresignedPutUrl(String s3Key, String contentType, long expirySeconds) {
        software.amazon.awssdk.services.s3.model.PutObjectRequest putReq = software.amazon.awssdk.services.s3.model.PutObjectRequest
                .builder()
                .bucket(s3Properties.getBucket())
                .key(s3Key)
                .contentType(contentType == null ? "application/octet-stream" : contentType)
                .build();

        software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest presignReq = software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
                .builder()
                .putObjectRequest(putReq)
                .signatureDuration(Duration.ofSeconds(Math.max(60, expirySeconds)))
                .build();

        software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest presigned = s3Presigner
                .presignPutObject(presignReq);
        return presigned.url();
    }

    /**
     * Batch upload multiple files for better performance
     */
    public List<FileDocument> uploadMultipleFiles(String entityType, String entityId,
            List<MultipartFile> files, String documentType,
            String uploadedByService, Integer uploadedByUserId) {
        return files.parallelStream()
                .map(file -> uploadFile(entityType, entityId, file, documentType, uploadedByService, uploadedByUserId,
                        Map.of()))
                .toList();
    }

    /**
     * Batch generate presigned URLs for multiple files
     */
    public Map<String, String> generateMultiplePresignedUrls(List<String> s3Keys, String operation) {
        return s3Keys.parallelStream()
                .collect(java.util.stream.Collectors.toMap(
                        s3Key -> s3Key,
                        s3Key -> generatePresignedUrl(s3Key, operation)));
    }

    /**
     * Batch delete multiple files
     */
    public void deleteMultipleFiles(List<String> docIds) {
        List<Integer> ids = docIds.stream()
                .map(Integer::valueOf)
                .toList();
        
        // Get all documents before deletion to retrieve S3 keys
        List<FileDocument> documents = fileDocumentRepository.findAllById(ids);
        
        // Delete from database
        fileDocumentRepository.deleteAllById(ids);
        
        // Delete from S3
        for (FileDocument document : documents) {
            if (document != null && document.getS3Key() != null) {
                try {
                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest = 
                        software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                            .bucket(s3Properties.getBucket())
                            .key(document.getS3Key())
                            .build();
                    
                    s3AsyncClient.deleteObject(deleteRequest);
                } catch (Exception e) {
                    // Log error but continue - file may already be deleted
                }
            }
        }
    }

    private String generateS3Key(String entityType, String entityId, String documentType, String filename) {
        String sanitizedFilename = FilenameSanitizer.sanitizeFilename(filename);

        // Global offers at root level: offers/{file}
        if (documentType != null && documentType.toLowerCase().contains("global_offer")) {
            return String.format("offers/%s", sanitizedFilename);
        }

        // Customer files: {merchantId}/customer/{customerId}/{subfolder}/{file}
        if ("customer".equalsIgnoreCase(entityType)) {
            String subfolder = getCustomerSubfolder(documentType);
            return String.format("%s/customer/%s/%s/%s",
                    entityId,
                    getMerchantIdFromContext(),
                    subfolder,
                    sanitizedFilename);
        }

        // Website files: {merchantId}/website/static/{subfolder}/{file}
        if (documentType != null && documentType.toLowerCase().startsWith("website_")) {
            String subfolder = documentType.substring(8);

            if ("root".equals(subfolder)) {
                return String.format("%s/website/%s", entityId, sanitizedFilename);
            }

            return String.format("%s/website/static/%s/%s",
                    entityId,
                    subfolder,
                    sanitizedFilename);
        }

        // Merchant files: {merchantId}/{folder}/{file}
        String folder = getDocumentTypeFolder(documentType);
        return String.format("%s/%s/%s", entityId, folder, sanitizedFilename);
    }

    private String getDocumentTypeFolder(String documentType) {
        if (documentType == null)
            return "documents";

        String docType = documentType.toLowerCase();

        if (docType.contains("banner"))
            return "banners";
        if (docType.contains("logo"))
            return "logos";
        if (docType.contains("profile"))
            return "profile_image";
        if (docType.contains("product"))
            return "product_image";
        if (docType.contains("menu"))
            return "menu_card";
        if (docType.contains("offer"))
            return "offers";

        return "documents";
    }

    private String getCustomerSubfolder(String documentType) {
        if (documentType == null)
            return PROFILE_IMG_FOLDER;

        String docType = documentType.toLowerCase();
        if (docType.contains("review"))
            return REVIEWS_FOLDER;
        if (docType.contains("profile"))
            return PROFILE_IMG_FOLDER;

        return PROFILE_IMG_FOLDER;
    }

    private String getMerchantIdFromContext() {
        return MERCHANT_DEFAULT;
    }


    private FileDocument createFileDocument(String entityType, String entityId, String uploadedByService,
            String documentType, String s3Key, MultipartFile file,
            Map<String, Object> tags, Integer uploadedByUserId) {
        FileDocument document = new FileDocument();
        document.setEntityType(entityType);
        document.setEntityId(entityId);
        document.setUploadedByService(uploadedByService);
        document.setDocumentType(documentType);
        document.setS3Key(s3Key);
        document.setS3Bucket(s3Properties.getBucket());
        document.setFileName(file.getOriginalFilename());
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        try {
            document.setTags(objectMapper.writeValueAsString(tags));
        } catch (Exception e) {
            document.setTags("{}");
        }
        document.setUploadedByUserId(uploadedByUserId);
        return document;
    }
}