package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.FileDocument;
import com.cloudkitchen.rbac.repository.FileDocumentRepository;
import com.cloudkitchen.rbac.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class FileServiceImpl implements FileService {

    private final S3Client s3Client;
    private final FileDocumentRepository fileDocumentRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public FileServiceImpl(S3Client s3Client, FileDocumentRepository fileDocumentRepository, ObjectMapper objectMapper) {
        this.s3Client = s3Client;
        this.fileDocumentRepository = fileDocumentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public FileDocument uploadFile(String entityType, String entityId, MultipartFile file, 
                                  String documentType, String uploadedByService, Integer uploadedByUserId, 
                                  Map<String, Object> tags) {
        try {
            // Generate S3 key based on database schema
            String s3Key = String.format("%s/%s/%s/%d_%s", 
                entityType, entityId, documentType, System.currentTimeMillis(), file.getOriginalFilename());
            
            // Upload to S3
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();
            
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            // Save metadata to database
            FileDocument document = new FileDocument();
            document.setEntityType(entityType);
            document.setEntityId(entityId);
            document.setUploadedByService(uploadedByService);
            document.setDocumentType(documentType);
            document.setS3Key(s3Key);
            document.setS3Bucket(bucketName);
            document.setFileName(file.getOriginalFilename());
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            document.setTags(objectMapper.writeValueAsString(tags));
            document.setUploadedByUserId(uploadedByUserId);
            
            return fileDocumentRepository.save(document);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
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
        try (S3Presigner presigner = S3Presigner.create()) {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(req -> req.bucket(bucketName).key(s3Key))
                .build();
            
            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }

    @Override
    public void deleteFile(String docId) {
        fileDocumentRepository.deleteById(Integer.valueOf(docId));
    }
}