package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.entity.*;
import com.cloudkitchen.rbac.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpAuditService.class);
    private final OtpLogRepository otpLogRepository;
    private final MerchantRepository merchantRepository;
    
    public OtpAuditService(OtpLogRepository otpLogRepository, MerchantRepository merchantRepository) {
        this.otpLogRepository = otpLogRepository;
        this.merchantRepository = merchantRepository;
    }
    
    public void logOtp(Integer merchantId, String phone, String otpCode, String otpType, String status, LocalDateTime expiresAt) {
        try {
            OtpLog otpLog = new OtpLog();
            
            if (merchantId != null) {
                Optional<Merchant> merchant = merchantRepository.findById(merchantId);
                merchant.ifPresent(otpLog::setMerchant);
            }
            
            otpLog.setPhone(phone);
            otpLog.setOtpCode(otpCode); // Store actual OTP for verification
            otpLog.setOtpType(otpType);
            otpLog.setStatus(status);
            // TODO: Extract actual IP and user agent from request context
            otpLog.setIpAddress("127.0.0.1"); // Valid INET format
            otpLog.setUserAgent("API-Request"); // Should be extracted from HttpServletRequest
            otpLog.setAttemptsCount(0);
            otpLog.setExpiresAt(expiresAt);
            
            otpLogRepository.save(otpLog);
            String maskedPhone = maskPhoneNumber(phone);
            logger.info("OTP audit log created for phone: {} with type: {} and status: {}", maskedPhone, otpType, status);

        } catch (Exception e) {
            logger.warn("OTP audit logging failed: {}", e.getMessage(), e);
        }
    }
    
    public void updateOtpVerified(String phone, String maskedOtp) {
        try {
            otpLogRepository.findTopByPhoneAndStatusOrderByCreatedOnDesc(phone, "sent")
                .ifPresent(otpLog -> {
                    otpLog.setStatus("verified");
                    otpLog.setVerifiedAt(LocalDateTime.now());
                    otpLogRepository.save(otpLog);
                    String maskedPhone = maskPhoneNumber(phone);
                    logger.info("OTP marked as verified for phone: {}", maskedPhone);
                });
        } catch (Exception e) {
            logger.warn("Failed to update OTP verification audit: {}", e.getMessage(), e);
        }
    }
    
    public void updateOtpFailed(String phone, int attemptCount) {
        try {
            otpLogRepository.findTopByPhoneAndStatusOrderByCreatedOnDesc(phone, "sent")
                .ifPresent(otpLog -> {
                    otpLog.setAttemptsCount(attemptCount);
                    if (attemptCount >= 3) {
                        otpLog.setStatus("failed");
                    } else {
                        otpLog.setStatus("invalid_attempt");
                    }
                    otpLogRepository.save(otpLog);
                    String maskedPhone = maskPhoneNumber(phone);
                    logger.info("OTP attempt count updated to {} for phone: {}, status: {}", 
                               attemptCount, maskedPhone, otpLog.getStatus());
                });
        } catch (Exception e) {
            logger.warn("Failed to update OTP failure audit: {}", e.getMessage(), e);
        }
    }
    
    public void updateOtpExpired(String phone) {
        try {
            otpLogRepository.findTopByPhoneAndStatusOrderByCreatedOnDesc(phone, "sent")
                .ifPresent(otpLog -> {
                    otpLog.setStatus("expired");
                    otpLogRepository.save(otpLog);
                    String maskedPhone = maskPhoneNumber(phone);
                    logger.info("OTP marked as expired for phone: {}", maskedPhone);
                });
        } catch (Exception e) {
            logger.warn("Failed to update OTP expiry audit: {}", e.getMessage(), e);
        }
    }
    
    public void updateOtpCancelled(String phone, String reason) {
        try {
            otpLogRepository.findTopByPhoneAndStatusOrderByCreatedOnDesc(phone, "sent")
                .ifPresent(otpLog -> {
                    otpLog.setStatus("cancelled");
                    otpLogRepository.save(otpLog);
                    String maskedPhone = maskPhoneNumber(phone);
                    logger.info("OTP cancelled for phone: {}, reason: {}", maskedPhone, reason);
                });
        } catch (Exception e) {
            logger.warn("Failed to update OTP cancellation audit: {}", e.getMessage(), e);
        }
    }
    
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}