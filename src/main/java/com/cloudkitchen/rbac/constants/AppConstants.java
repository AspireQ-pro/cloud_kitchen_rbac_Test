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

    // S3 Folder Structure Constants (Merchant-Centric)
    public static class S3Folders {
        // Merchant root folders
        public static final String BANNERS = "banners";
        public static final String LOGO = "logo";
        public static final String PROFILE_IMAGE = "profile_image";
        public static final String PRODUCT_IMAGE = "product_image";
        public static final String MENU_CARD = "menu_card";
        public static final String OFFERS = "offers";
        public static final String CUSTOMER = "customer";
        public static final String DOCUMENTS = "documents";
        
        // Customer sub-folders
        public static final String CUSTOMER_PROFILE_IMG = "profile_img";
        public static final String CUSTOMER_REVIEWS = "reviews";
        
        // Website sub-folders (only static assets)
        public static final String WEBSITE = "website";
        public static final String WEBSITE_STATIC = "website/static";
        public static final String WEBSITE_STATIC_CSS = "website/static/css";
        public static final String WEBSITE_STATIC_JS = "website/static/js";
        public static final String WEBSITE_STATIC_IMAGES = "website/static/images";
    }

    // File Naming Conventions
    public static class FileNaming {
        public static final String SEPARATOR = "_";
        public static final String EXTENSION_SEPARATOR = ".";
        public static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";
        
        // Standard filenames for website
        public static final String LOGO_FILE = "logo.png";
        public static final String BANNER_FILE = "banner.jpg";
        public static final String PHOTO_FILE = "photo.jpg";
        public static final String INDEX_FILE = "index.html";
        public static final String CONFIG_FILE = "config.json";
    }

    // S3 Performance Constants
    public static class S3Performance {
        public static final long PRESIGNED_URL_TTL = 50 * 60 * 1000L; // 50 minutes
        public static final long CACHE_CLEANUP_INTERVAL = 60 * 60 * 1000L; // 1 hour
        public static final int MAX_CONCURRENT_UPLOADS = 10;
        public static final long MULTIPART_THRESHOLD = 5 * 1024 * 1024L; // 5MB
    }
    
    // Document Types
    public static class DocumentTypes {
        // Merchant root level
        public static final String BANNER = "banner";
        public static final String LOGO = "logo";
        public static final String PROFILE_IMAGE = "profile_image";
        public static final String PRODUCT_IMAGE = "product_image";
        public static final String MENU_CARD = "menu_card";
        public static final String OFFER = "offer";
        
        // Customer document types
        public static final String CUSTOMER_PROFILE = "customer_profile";
        public static final String CUSTOMER_REVIEW = "customer_review";
        
        // Website document types
        public static final String WEBSITE_ROOT = "website_root";        // For index.html, config.json
        public static final String WEBSITE_STATIC_CSS = "website_static/css";   // For website/static/css/
        public static final String WEBSITE_STATIC_JS = "website_static/js";     // For website/static/js/
        public static final String WEBSITE_STATIC_IMAGES = "website_static/images"; // For website/static/images/
    }
}
