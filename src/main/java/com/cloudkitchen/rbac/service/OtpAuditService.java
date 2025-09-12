package com.cloudkitchen.rbac.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class OtpAuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpAuditService.class);
    private final JdbcTemplate jdbcTemplate;
    
    public OtpAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public void logOtp(Integer merchantId, String phone, String otpCode, LocalDateTime expiresAt) {
        try {
            String sql = """
                INSERT INTO otp_logs (merchant_id, phone, otp_code, otp_type, status,
                                    ip_address, user_agent, attempts_count, expires_at)
                VALUES (?, ?, ?, 'login', 'sent', '127.0.0.1'::inet, 'API-Request', 0, ?)
                """;

            jdbcTemplate.update(sql, merchantId, phone, otpCode, expiresAt);
            logger.info("OTP audit log created successfully");

        } catch (Exception e) {
            logger.error("OTP audit logging failed", e);
        }
    }
    
    public void updateOtpVerified(String phone, String otpCode) {
        try {
            // First check if record exists
            String checkSql = "SELECT COUNT(*) FROM otp_logs WHERE phone = ? AND otp_code = ? AND status = 'sent'";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, phone, otpCode);
            logger.debug("Found {} records to update for verification", count);
            
            if (count > 0) {
                String sql = "UPDATE otp_logs SET status = 'verified', verified_at = CURRENT_TIMESTAMP WHERE phone = ? AND otp_code = ? AND status = 'sent'";
                int updated = jdbcTemplate.update(sql, phone, otpCode);
                logger.info("OTP marked as verified. Rows updated: {}", updated);
            }
        } catch (Exception e) {
            logger.error("Failed to update OTP verification", e);
        }
    }
    
    public void updateOtpFailed(String phone, int attemptCount) {
        try {
            // First check if record exists
            String checkSql = "SELECT COUNT(*) FROM otp_logs WHERE phone = ? AND status = 'sent'";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, phone);
            logger.debug("Found {} records to update for failed attempt", count);
            
            if (count > 0) {
                String sql = "UPDATE otp_logs SET attempts_count = ? WHERE phone = ? AND status = 'sent'";
                int updated = jdbcTemplate.update(sql, attemptCount, phone);
                logger.info("OTP attempt count updated to {}. Rows updated: {}", attemptCount, updated);
            }
        } catch (Exception e) {
            logger.error("Failed to update OTP failure", e);
        }
    }
}