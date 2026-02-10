package com.cloudkitchen.rbac.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.OtpLog;
import com.cloudkitchen.rbac.repository.OtpLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class OtpAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpAuditService.class);
    private final OtpLogRepository otpLogRepository;
    
    public OtpAuditService(OtpLogRepository otpLogRepository) {
        this.otpLogRepository = otpLogRepository;
    }
    
    public void logOtp(Integer merchantId, String phone, String otpCode, String otpType, String status, LocalDateTime expiresAt) {
        try {
            OtpLog otpLog = new OtpLog();
            
            if (merchantId != null) {
                Merchant merchant = new Merchant();
                merchant.setMerchantId(merchantId);
                otpLog.setMerchant(merchant);
            }
            
            otpLog.setPhone(phone);
            otpLog.setOtpCode(otpCode); // Store actual OTP for development/testing
            otpLog.setOtpType(otpType);
            otpLog.setStatus(status);
            
            // Extract IP address and user agent from HTTP request
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();
            
            otpLog.setIpAddress(ipAddress);
            otpLog.setUserAgent(userAgent);
            otpLog.setAttemptsCount(0);
            otpLog.setExpiresAt(expiresAt);
            
            otpLogRepository.save(otpLog);
            String maskedPhone = maskPhoneNumber(phone);
            logger.info("OTP audit log created for phone: {} with type: {} and status: {}", maskedPhone, otpType, status);

        } catch (Exception e) {
            String maskedPhone = maskPhoneNumber(phone);
            logger.error("OTP audit logging failed for phone: {}, type: {}", maskedPhone, otpType, e);
        }
    }
    
    public void logOtpVerified(String phone, Integer merchantId) {
        try {
            otpLogRepository.findTopByPhoneAndStatusOrderByCreatedOnDesc(phone, "sent")
                .ifPresentOrElse(otpLog -> {
                    otpLog.setStatus("verified");
                    otpLog.setVerifiedAt(LocalDateTime.now());
                    otpLogRepository.save(otpLog);
                    String maskedPhone = maskPhoneNumber(phone);
                    logger.info("OTP verification logged for phone: {}", maskedPhone);
                }, () -> {
                    OtpLog otpLog = new OtpLog();
                    
                    if (merchantId != null) {
                        Merchant merchant = new Merchant();
                        merchant.setMerchantId(merchantId);
                        otpLog.setMerchant(merchant);
                    }
                    
                    otpLog.setPhone(phone);
                    otpLog.setOtpCode("0000"); // 4 chars to satisfy column constraint
                    otpLog.setOtpType("login"); // Fallback type for verification log
                    otpLog.setStatus("verified");
                    otpLog.setVerifiedAt(LocalDateTime.now());
                    
                    String ipAddress = getClientIpAddress();
                    String userAgent = getUserAgent();
                    
                    otpLog.setIpAddress(ipAddress);
                    otpLog.setUserAgent(userAgent);
                    otpLog.setAttemptsCount(0);
                    otpLog.setExpiresAt(LocalDateTime.now().plusMinutes(1)); // Short expiry for verification log
                    
                    otpLogRepository.save(otpLog);
                    String maskedPhone = maskPhoneNumber(phone);
                    logger.info("OTP verification logged for phone (no prior sent log): {}", maskedPhone);
                });
        } catch (Exception e) {
            logger.warn("Failed to log OTP verification: {}", e.getMessage(), e);
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
                    otpLog.setStatus("expired");
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
    
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Check for IP address from various headers (for proxy/load balancer scenarios)
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("HTTP_CLIENT_IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
                
                // Handle multiple IPs in X-Forwarded-For header
                if (ipAddress != null && ipAddress.contains(",")) {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
                
                // Format IPv6 localhost to IPv4
                if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
                    ipAddress = "127.0.0.1";
                }
                
                return ipAddress != null ? ipAddress : "127.0.0.1";
            }
        } catch (Exception e) {
            logger.warn("Failed to extract IP address: {}", e.getMessage());
        }
        return "127.0.0.1";
    }
    
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userAgent = request.getHeader("User-Agent");
                return userAgent != null ? userAgent : "Unknown";
            }
        } catch (Exception e) {
            logger.warn("Failed to extract User-Agent: {}", e.getMessage());
        }
        return "Unknown";
    }
}
