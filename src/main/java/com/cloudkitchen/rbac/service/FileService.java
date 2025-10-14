package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.entity.FileDocument;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface FileService {
    
    FileDocument uploadFile(String entityType, String entityId, MultipartFile file, 
                           String documentType, String uploadedByService, Integer uploadedByUserId, 
                           Map<String, Object> tags);
    
    List<FileDocument> getFiles(String entityType, String entityId, Map<String, Object> filters);
    
    String generatePresignedUrl(String s3Key, String operation);
    
    void deleteFile(String docId);
}