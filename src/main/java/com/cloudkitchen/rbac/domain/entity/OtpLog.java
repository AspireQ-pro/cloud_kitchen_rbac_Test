package com.cloudkitchen.rbac.domain.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "otp_logs")
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

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type", nullable = false, length = 20)
    private OtpType otpType = OtpType.LOGIN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OtpStatus status = OtpStatus.SENT;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "attempts_count", nullable = false)
    private Integer attemptsCount = 0;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_on", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdOn;

    // Setters
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
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }
}
