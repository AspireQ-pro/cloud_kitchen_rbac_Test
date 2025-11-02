package com.cloudkitchen.rbac.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadOfferImage(String merchantId, MultipartFile file, boolean global);
    String uploadBannerImage(String merchantId, MultipartFile file);
    String uploadProductImage(String merchantId, MultipartFile file);
    String uploadProfileImage(String merchantId, MultipartFile file);
    String uploadMenuCardImage(String merchantId, MultipartFile file);
}