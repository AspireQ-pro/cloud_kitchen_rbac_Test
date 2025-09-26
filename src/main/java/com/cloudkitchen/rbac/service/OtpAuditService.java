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
    
    public void logOtp(Integer merchantId, String phone, String otpCode, LocalDateTime expiresAt) {
        try {
            OtpLog otpLog = new OtpLog();
            
            if (merchantId != null) {
                Optional<Merchant> merchant = merchantRepository.findById(merchantId);
                merchant.ifPresent(otpLog::setMerchant);
            }
            
            otpLog.setPhone(phone);
            otpLog.setOtpCode("****");
            otpLog.setOtpType(OtpLog.OtpType.LOGIN);
            otpLog.setStatus(OtpLog.OtpStatus.SENT);
            otpLog.setIpAddress("127.0.0.1");
            otpLog.setUserAgent("API-Request");
            otpLog.setAttemptsCount(0);
            otpLog.setExpiresAt(expiresAt);
            
            otpLogRepository.save(otpLog);
            String maskedPhone = maskPhoneNumber(phone);
            logger.info("OTP audit log created for phone: {}", maskedPhone);

        } catch (Exception e) {
            logger.warn("OTP audit logging failed: {}", e.getMessage(), e);
        }
    }
    
    public void updateOtpVerified(String phone, String maskedOtp) {
        try {
            otpLogRepository.findTopByPhoneAndStatusOrderByCreatedOnDesc(phone, OtpLog.OtpStatus.SENT)
                .ifPresent(otpLog -> {
                    otpLog.setStatus(OtpLog.OtpStatus.VERIFIED);
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
            otpLogRepository.findTopByPhoneAndStatusOrderByCreatedOnDesc(phone, OtpLog.OtpStatus.SENT)
                .ifPresent(otpLog -> {
                    otpLog.setAttemptsCount(attemptCount);
                    otpLogRepository.save(otpLog);
                    String maskedPhone = maskPhoneNumber(phone);
                    logger.info("OTP attempt count updated to {} for phone: {}", attemptCount, maskedPhone);
                });
        } catch (Exception e) {
            logger.warn("Failed to update OTP failure audit: {}", e.getMessage(), e);
        }
    }
    
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}