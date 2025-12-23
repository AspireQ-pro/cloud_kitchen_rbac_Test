package com.cloudkitchen.rbac.constants;

public final class AppConstants {
    
    private AppConstants() {
        // Utility class
    }
    
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

    // S3 Folder Structure Constants (RBAC Service Specific)
    public static class S3Folders {
        // Global folders (root level)
        public static final String OFFERS = "offers";
        public static final String ADS = "ads";
        
        // Merchant-specific folders
        public static final String BANNERS = "banners";
        public static final String LOGOS = "logos";
        public static final String PROFILE_IMAGE = "profile_image";
        public static final String PRODUCT_IMAGE = "product_image";
        public static final String MENU_CARD = "menu_card";
        
        // Customer sub-folders
        public static final String CUSTOMER = "customer";
        public static final String CUSTOMER_PROFILE_IMG = "profile_img";
        public static final String CUSTOMER_REVIEWS = "reviews";
    }

    // File Naming Conventions
    public static class FileNaming {
        public static final String SEPARATOR = "_";
        public static final String EXTENSION_SEPARATOR = ".";
        public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
        public static final String FOLDER_PLACEHOLDER = ".keep";
    }

    // S3 Performance Constants
    public static class S3Performance {
        public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
        public static final int MAX_KEY_LENGTH = 1024;
        public static final int MAX_ID_LENGTH = 100;
    }
    
    // File Validation
    public static class FileValidation {
        public static final String ALLOWED_CONTENT_TYPE_PREFIX = "image/";
        public static final String VALID_ID_PATTERN = "^[a-zA-Z0-9_-]+$";
        public static final String FOLDER_PLACEHOLDER_TEXT = "# folder placeholder";
    }
}
