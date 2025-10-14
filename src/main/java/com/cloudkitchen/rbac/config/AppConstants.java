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
}