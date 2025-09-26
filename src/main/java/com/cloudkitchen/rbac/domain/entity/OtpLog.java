package com.cloudkitchen.rbac.domain.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "otp_logs",
    indexes = {
        @Index(name = "idx_otp_logs_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_otp_logs_phone", columnList = "phone"),
        @Index(name = "idx_otp_logs_phone_status", columnList = "phone, status"),
        @Index(name = "idx_otp_logs_expires_at", columnList = "expires_at"),
        @Index(name = "idx_otp_logs_merchant_phone", columnList = "merchant_id, phone")
    }
)
public class OtpLog {

    public enum OtpType {
        LOGIN, REGISTRATION, PASSWORD_RESET, PHONE_VERIFICATION
    }

    public enum OtpStatus {
        SENT, VERIFIED, EXPIRED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_log_id")
    private Integer otpLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "otp_code", nullable = false, length = 4)
    private String otpCode; // Store hashed OTP in production

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type", nullable = false, length = 20)
    private OtpType otpType = OtpType.LOGIN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OtpStatus status = OtpStatus.SENT;

    @Column(name = "ip_address")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.OTHER)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "attempts_count", nullable = false)
    private Integer attemptsCount = 0;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_on", updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdOn;

    // Getters and Setters
    public Integer getOtpLogId() { return otpLogId; }
    public Merchant getMerchant() { return merchant; }
    public String getPhone() { return phone; }
    // Removed getter for security - OTP should not be exposed
    // Use service methods for OTP validation instead
    public OtpType getOtpType() { return otpType; }
    public OtpStatus getStatus() { return status; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Integer getAttemptsCount() { return attemptsCount; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setOtpLogId(Integer otpLogId) { this.otpLogId = otpLogId; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    public void setOtpType(OtpType otpType) { this.otpType = otpType; }
    public void setStatus(OtpStatus status) { this.status = status; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setAttemptsCount(Integer attemptsCount) { this.attemptsCount = attemptsCount; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }
}
