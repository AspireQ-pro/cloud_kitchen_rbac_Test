package com.cloudkitchen.rbac.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    
    public boolean sendOtp(String phone, String otp) {
        try {
            if (phone == null || phone.isBlank()) {
                throw new IllegalArgumentException("Phone number cannot be null or empty");
            }
            if (otp == null || otp.isBlank()) {
                throw new IllegalArgumentException("OTP cannot be null or empty");
            }
            String maskedPhone = phone.length() > 4 ? "****" + phone.substring(phone.length() - 4) : "****";
            logger.info("OTP sent to {} - Code: **** (Valid: 5min)", maskedPhone);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send OTP: {}", e.getMessage());
            return false;
        }
    }
}