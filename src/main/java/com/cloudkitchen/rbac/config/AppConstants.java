package com.cloudkitchen.rbac.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppConstants {
    
    // OTP Configuration
    public static final int OTP_MAX_ATTEMPTS = 3;
    public static final int OTP_RATE_LIMIT_REQUESTS = 3;
    public static final int OTP_RATE_LIMIT_WINDOW_MINUTES = 30;
    public static final int OTP_EXPIRY_MINUTES = 5;
    
    // JWT Configuration
    public static final int JWT_ACCESS_TOKEN_EXPIRY_SECONDS = 1800; // 30 minutes
    public static final int JWT_REFRESH_TOKEN_EXPIRY_SECONDS = 259200; // 3 days
    
    // Rate Limiting
    public static final int RATE_LIMIT_MAX_REQUESTS = 10;
    public static final long RATE_LIMIT_WINDOW_MS = 60000; // 1 minute
    
    // System User ID for initialization
    public static final int SYSTEM_USER_ID = 0;
    

    
    // Default Values
    @Value("${app.default.merchant.name:Test Merchant}")
    private String defaultMerchantName;
    
    @Value("${app.default.merchant.email:admin@testmerchant.com}")
    private String defaultMerchantEmail;
    
    @Value("${app.default.merchant.phone:9876543210}")
    private String defaultMerchantPhone;
    
    @Value("${app.default.merchant.address:123 Test Street, Test City}")
    private String defaultMerchantAddress;
    
    public String getDefaultMerchantName() { return defaultMerchantName; }
    public String getDefaultMerchantEmail() { return defaultMerchantEmail; }
    public String getDefaultMerchantPhone() { return defaultMerchantPhone; }
    public String getDefaultMerchantAddress() { return defaultMerchantAddress; }

    // S3 Folder Structure Constants (Industry Standard)
    public static class S3Folders {
        // Base folders
        public static final String MERCHANTS = "merchants";
        public static final String CUSTOMERS = "customers";
        public static final String PRODUCTS = "products";
        public static final String ORDERS = "orders";
        public static final String USERS = "users";
        public static final String TEMP = "temp";
        public static final String ARCHIVE = "archive";
        public static final String PUBLIC = "public";
        public static final String PRIVATE = "private";

        // Sub-folders for merchants
        public static final String MERCHANT_BANNERS = "banners";
        public static final String MERCHANT_LOGOS = "logos";
        public static final String MERCHANT_DOCUMENTS = "documents";
        public static final String MERCHANT_PRODUCTS = "products";
        public static final String MERCHANT_STAFF = "staff";

        // Sub-folders for customers
        public static final String CUSTOMER_PROFILES = "profiles";
        public static final String CUSTOMER_DOCUMENTS = "documents";
        public static final String CUSTOMER_ORDERS = "orders";
        public static final String CUSTOMER_REVIEWS = "reviews";

        // File type folders
        public static final String IMAGES = "images";
        public static final String VIDEOS = "videos";
        public static final String DOCUMENTS = "documents";
        public static final String AUDIOS = "audios";
        public static final String THUMBNAILS = "thumbnails";

        // Size variants
        public static final String ORIGINAL = "original";
        public static final String RESIZED = "resized";
        public static final String COMPRESSED = "compressed";
    }

    // File Naming Conventions
    public static class FileNaming {
        public static final String SEPARATOR = "_";
        public static final String EXTENSION_SEPARATOR = ".";
        public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";

        // File type prefixes
        public static final String PROFILE_PREFIX = "profile";
        public static final String BANNER_PREFIX = "banner";
        public static final String LOGO_PREFIX = "logo";
        public static final String PRODUCT_PREFIX = "product";
        public static final String DOCUMENT_PREFIX = "doc";
        public static final String THUMBNAIL_PREFIX = "thumb";
    }

    // S3 Performance Constants
    public static class S3Performance {
        public static final long PRESIGNED_URL_TTL = 50 * 60 * 1000L; // 50 minutes
        public static final long CACHE_CLEANUP_INTERVAL = 60 * 60 * 1000L; // 1 hour
        public static final int MAX_CONCURRENT_UPLOADS = 10;
        public static final long MULTIPART_THRESHOLD = 5 * 1024 * 1024L; // 5MB
    }
}