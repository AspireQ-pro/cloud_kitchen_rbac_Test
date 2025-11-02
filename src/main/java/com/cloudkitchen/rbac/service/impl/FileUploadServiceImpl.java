package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.exception.BusinessExceptions.FileUploadException;
import com.cloudkitchen.rbac.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadServiceImpl.class);
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.url}")
    private String s3BaseUrl;

    public FileUploadServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadOfferImage(String merchantId, MultipartFile file, boolean global) {
        validateFile(file);
        String folderPath = global ? "offers/" : merchantId + "/offers/";
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folderPath + fileName;
        
        return uploadToS3(file, key, "offer image");
    }

    @Override
    public String uploadBannerImage(String merchantId, MultipartFile file) {
        validateFile(file);
        String folderPath = merchantId + "/banners/";
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folderPath + fileName;
        
        return uploadToS3(file, key, "banner image");
    }

    @Override
    public String uploadProductImage(String merchantId, MultipartFile file) {
        validateFile(file);
        String folderPath = merchantId + "/product_image/";
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folderPath + fileName;
        
        return uploadToS3(file, key, "product image");
    }

    @Override
    public String uploadProfileImage(String merchantId, MultipartFile file) {
        validateFile(file);
        String folderPath = merchantId + "/profile_image/";
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folderPath + fileName;
        
        return uploadToS3(file, key, "profile image");
    }

    @Override
    public String uploadMenuCardImage(String merchantId, MultipartFile file) {
        validateFile(file);
        String folderPath = merchantId + "/menu_card/";
        String fileName = generateFileName(file.getOriginalFilename());
        String key = folderPath + fileName;
        
        return uploadToS3(file, key, "menu card image");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new FileUploadException("Only JPEG, JPG, PNG, and WebP images are allowed");
        }
    }

    private String generateFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFilename);
        return timestamp + "_" + uuid + extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // default extension
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private String uploadToS3(MultipartFile file, String key, String fileType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            String fileUrl = s3BaseUrl + key;
            logger.info("Successfully uploaded {} to S3: {}", fileType, key);
            return fileUrl;
            
        } catch (IOException e) {
            logger.error("Failed to upload {} to S3 due to IO error: {}", fileType, e.getMessage(), e);
            throw new FileUploadException("Failed to upload " + fileType + " due to IO error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error uploading {} to S3: {}", fileType, e.getMessage(), e);
            throw new FileUploadException("Failed to upload " + fileType + " due to unexpected error: " + e.getMessage());
        }
    }
}