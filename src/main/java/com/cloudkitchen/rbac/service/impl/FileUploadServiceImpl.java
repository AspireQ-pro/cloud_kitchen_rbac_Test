package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.config.S3Properties;
import com.cloudkitchen.rbac.exception.BusinessExceptions.FileUploadException;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.apache.tika.Tika;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadServiceImpl.class);
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png",
            "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final JwtTokenProvider jwtTokenProvider;
    private final Tika tika = new Tika();

    public FileUploadServiceImpl(S3Client s3Client, S3Properties s3Properties, JwtTokenProvider jwtTokenProvider) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String uploadOfferImage(String merchantId, MultipartFile file, boolean global) {
        validateFile(file);
        if (global) {
            // Global offers at root: offers/{file}
            String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
            String key = "offers/" + sanitizedFilename;
            return uploadToS3(file, key, "global offer image");
        }
        String resolvedMerchantId = resolveMerchantId(merchantId);
        String key = generateS3Key(resolvedMerchantId, "offer", file.getOriginalFilename(), null);
        return uploadToS3(file, key, "offer image");
    }

    @Override
    public String uploadAdImage(String merchantId, MultipartFile file, boolean global) {
        validateFile(file);
        if (global) {
            // Global ads at root: ads/{file}
            String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
            String key = "ads/" + sanitizedFilename;
            return uploadToS3(file, key, "global ad image");
        }
        String resolvedMerchantId = resolveMerchantId(merchantId);
        String key = generateS3Key(resolvedMerchantId, "ad", file.getOriginalFilename(), null);
        return uploadToS3(file, key, "ad image");
    }

    @Override
    public String uploadBannerImage(String merchantId, MultipartFile file) {
        validateFile(file);
        String resolvedMerchantId = resolveMerchantId(merchantId);
        String key = generateS3Key(resolvedMerchantId, "banner", file.getOriginalFilename(), null);
        return uploadToS3(file, key, "banner image");
    }

    @Override
    public String uploadProductImage(String merchantId, MultipartFile file) {
        validateFile(file);
        String resolvedMerchantId = resolveMerchantId(merchantId);
        String key = generateS3Key(resolvedMerchantId, "product", file.getOriginalFilename(), null);
        return uploadToS3(file, key, "product image");
    }

    @Override
    public String uploadProfileImage(String merchantId, MultipartFile file) {
        validateFile(file);
        String resolvedMerchantId = resolveMerchantId(merchantId);
        String key = generateS3Key(resolvedMerchantId, "profile", file.getOriginalFilename(), null);
        return uploadToS3(file, key, "profile image");
    }

    @Override
    public String uploadMenuCardImage(String merchantId, MultipartFile file) {
        validateFile(file);
        String resolvedMerchantId = resolveMerchantId(merchantId);
        String key = generateS3Key(resolvedMerchantId, "menu", file.getOriginalFilename(), null);
        return uploadToS3(file, key, "menu card image");
    }

    @Override
    public String uploadFile(String merchantId, String documentType, MultipartFile file) {
        validateFile(file);
        String resolvedMerchantId = resolveMerchantId(merchantId);
        String key = generateS3Key(resolvedMerchantId, documentType, file.getOriginalFilename(), null);
        return uploadToS3(file, key, documentType + " file");
    }

    @Override
    public String uploadCustomerFile(String merchantId, String customerId, String documentType, MultipartFile file) {
        validateFile(file);
        String resolvedMerchantId = resolveMerchantId(merchantId);

        if (customerId == null || customerId.isBlank()) {
            throw new FileUploadException("Customer ID is required for customer file uploads");
        }

        String key = generateS3Key(resolvedMerchantId, documentType, file.getOriginalFilename(), customerId);
        return uploadToS3(file, key, "customer " + documentType);
    }

    @Override
    public String uploadWebsiteFile(String merchantId, String documentType, MultipartFile file) {
        // Website files may include HTML, CSS, JS, JSON - different validation
        validateWebsiteFile(file);
        String resolvedMerchantId = resolveMerchantId(merchantId);
        String key = generateS3Key(resolvedMerchantId, documentType, file.getOriginalFilename(), null);
        return uploadToS3(file, key, "website " + documentType);
    }

    /**
     * Generates S3 key with correct folder structure based on document type.
     * Always prefixes with {merchantId}/ for proper organization.
     * 
     * @param merchantId       Merchant identifier
     * @param documentType     Type of document (banner, logo, product, menu, offer,
     *                         website_root, website_static, etc.)
     * @param originalFilename Original file name
     * @param customerId       Customer ID (optional, for customer-specific files)
     * @return Complete S3 key path
     */
    private String generateS3Key(String merchantId, String documentType, String originalFilename, String customerId) {
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String folder = mapDocumentTypeToFolder(documentType);

        // Customer-specific files: {merchantId}/customer/{customerId}/{folder}/{file}
        if (customerId != null && !customerId.isBlank()) {
            return String.format("%s/customer/%s/%s%s", merchantId, customerId, folder, sanitizedFilename);
        }

        // Regular merchant files: {merchantId}/{folder}/{file}
        return String.format("%s/%s%s", merchantId, folder, sanitizedFilename);
    }

    /**
     * Maps document type to correct S3 folder path.
     * Ensures consistent folder structure across the application.
     */
    private String mapDocumentTypeToFolder(String documentType) {
        if (documentType == null) {
            throw new FileUploadException("Document type is required");
        }

        return switch (documentType.toLowerCase()) {
            case "banner" -> "banners/";
            case "logo" -> "logos/";
            case "profile" -> "profile_image/";
            case "product" -> "product_image/";
            case "menu" -> "menu_card/";
            case "offer" -> "offers/";
            case "ad" -> "ads/";
            case "website_root" -> "website/";
            case "website_static" -> "website/static/";
            case "website_css" -> "website/static/css/";
            case "website_js" -> "website/static/js/";
            case "website_images" -> "website/static/images/";
            case "customer_profile" -> "profile_img/";
            case "customer_review" -> "reviews/";
            default -> throw new FileUploadException("Invalid document type: " + documentType);
        };
    }

    /**
     * Resolves merchantId from multiple sources:
     * 1. Provided merchantId parameter
     * 2. JWT token in SecurityContext
     * 3. Fallback to "merchant_default" for test cases only
     */
    private String resolveMerchantId(String providedMerchantId) {
        // Use provided merchantId if valid
        if (providedMerchantId != null && !providedMerchantId.isBlank() && !providedMerchantId.equals("null")) {
            return providedMerchantId;
        }

        // Extract from JWT token
        Integer merchantIdFromJwt = getMerchantIdFromContext();
        if (merchantIdFromJwt != null) {
            return String.valueOf(merchantIdFromJwt);
        }

        // Fallback for test cases only
        logger.warn("No merchantId found in request or JWT. Using fallback 'merchant_default'");
        return "merchant_default";
    }

    /**
     * Extracts merchantId from JWT token in SecurityContext.
     * Returns null if not available or invalid.
     */
    private Integer getMerchantIdFromContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object credentials = authentication.getCredentials();
            if (credentials instanceof String token) {
                return jwtTokenProvider.getMerchantId(token);
            }

            return null;
        } catch (Exception e) {
            logger.debug("Failed to extract merchantId from JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sanitizes filename to ensure consistent, safe S3 keys.
     * - Converts to lowercase
     * - Replaces spaces with underscores
     * - Removes invalid characters
     * - Prevents double underscores
     * - Preserves file extension
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return generateUniqueFilename(".jpg");
        }

        String extension = getFileExtension(filename);
        String nameWithoutExt = filename.substring(0,
                filename.lastIndexOf('.') > 0 ? filename.lastIndexOf('.') : filename.length());

        // Sanitize: lowercase, replace spaces, remove invalid chars
        String sanitized = nameWithoutExt.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_-]", "")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_+|_+$", "");

        // If sanitization removed everything, generate new name
        if (sanitized.isBlank()) {
            return generateUniqueFilename(extension);
        }

        // Add timestamp and UUID for uniqueness
        return generateUniqueFilename(sanitized + extension);
    }

    private String generateUniqueFilename(String filename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(filename);
        String nameWithoutExt = filename.replace(extension, "");

        if (nameWithoutExt.isBlank()) {
            return timestamp + "_" + uuid + extension;
        }

        return nameWithoutExt + "_" + timestamp + "_" + uuid + extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
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

        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!ALLOWED_IMAGE_TYPES.contains(detectedType)) {
                throw new FileUploadException("Invalid file type detected: " + detectedType);
            }
        } catch (IOException e) {
            throw new FileUploadException("Failed to validate file content");
        }
    }

    private void validateWebsiteFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        List<String> allowedWebTypes = Arrays.asList(
                "text/html", "text/css", "application/javascript", "text/javascript",
                "application/json", "image/jpeg", "image/png", "image/webp", "image/svg+xml");

        if (contentType == null || !allowedWebTypes.contains(contentType.toLowerCase())) {
            throw new FileUploadException("Invalid file type for website upload");
        }
    }

    /**
     * Uploads file to S3 with idempotent behavior.
     * Calling upload again with same key will overwrite previous file.
     * S3 automatically creates folders when uploading files with paths.
     */
    private String uploadToS3(MultipartFile file, String key, String fileType) {
        try {
            // S3 automatically creates folder structure from the key path
            // No need to explicitly create folders - they're virtual in S3
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = buildS3Url(key);
            logger.info("Successfully uploaded {} to S3: {}", fileType, key);
            return fileUrl;

        } catch (IOException e) {
            logger.error("Failed to upload {} to S3 due to IO error: {}", fileType, e.getMessage(), e);
            throw new FileUploadException("Failed to upload " + fileType + " due to IO error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error uploading {} to S3: {}", fileType, e.getMessage(), e);
            throw new FileUploadException(
                    "Failed to upload " + fileType + " due to unexpected error: " + e.getMessage());
        }
    }

    private String buildS3Url(String key) {
        if (s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().isBlank()) {
            // LocalStack or custom endpoint
            return s3Properties.getEndpoint() + "/" + s3Properties.getBucket() + "/" + key;
        }
        // AWS S3 - region-specific URL
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Properties.getBucket(), s3Properties.getRegion(), key);
    }
}
