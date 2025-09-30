package com.cloudkitchen.rbac.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_logs",
    indexes = {
        @Index(name = "idx_otp_logs_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_otp_logs_phone", columnList = "phone"),
        @Index(name = "idx_otp_logs_phone_status", columnList = "phone, status"),
        @Index(name = "idx_otp_logs_expires_at", columnList = "expires_at"),
        @Index(name = "idx_otp_logs_created_on", columnList = "created_on"),
        @Index(name = "idx_otp_logs_merchant_phone", columnList = "merchant_id, phone")
    }
)
public class OtpLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_log_id")
    private Integer otpLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "otp_code", length = 4, nullable = false)
    private String otpCode;

    @Column(name = "otp_type", length = 20)
    private String otpType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "ip_address", length = 45, columnDefinition = "VARCHAR(45)")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "attempts_count")
    private Integer attemptsCount = 0;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn = LocalDateTime.now();

    // Getters and Setters
    public Integer getOtpLogId() { return otpLogId; }
    public void setOtpLogId(Integer otpLogId) { this.otpLogId = otpLogId; }

    public Merchant getMerchant() { return merchant; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // OTP code should only be used for verification, not retrieval
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    
    public boolean verifyOtp(String inputOtp) {
        return this.otpCode != null && this.otpCode.equals(inputOtp);
    }

    public String getOtpType() { return otpType; }
    public void setOtpType(String otpType) { this.otpType = otpType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Integer getAttemptsCount() { return attemptsCount; }
    public void setAttemptsCount(Integer attemptsCount) { this.attemptsCount = attemptsCount; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }
}
