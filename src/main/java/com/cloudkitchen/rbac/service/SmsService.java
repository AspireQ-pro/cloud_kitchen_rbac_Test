package com.cloudkitchen.rbac.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    
    public void sendOtp(String phone, String otp) {
        logger.info("=== OTP NOTIFICATION ===");
        logger.info("Phone: +91{}", phone);
        logger.info("OTP Code: ****");
        logger.info("Valid for: 5 minutes");
        logger.info("========================");
    }
}