package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OTP verification request")
public class OtpVerifyRequest {
    @NotNull(message = "Merchant ID is required")
    @Min(value = 0, message = "Merchant ID must be 0 or positive")
    @Schema(description = "Merchant ID (0 for phone-based verification, >0 for merchant-specific)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer merchantId;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number format")
    @Length(min = 10, max = 13, message = "Phone number must be 10-13 characters")
    @Schema(description = "Phone number (10-digit Indian mobile)", example = "9075027004", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;
    
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{4}$", message = "OTP must be 4 digits")
    @Length(min = 4, max = 4, message = "OTP must be exactly 4 digits")
    @Schema(description = "4-digit OTP code", example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otp;
    
    @Pattern(regexp = "^(login|password_reset|phone_verification|account_verification)$", 
             message = "OTP type must be one of: login, password_reset, phone_verification, account_verification")
    @Schema(description = "Type of OTP verification", example = "login", allowableValues = {"login", "password_reset", "phone_verification", "account_verification"})
    private String otpType = "login"; // login, password_reset, phone_verification, account_verification

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        this.phone = phone != null ? phone.trim().replaceAll("\\s+", "") : null; 
    }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { 
        this.otp = otp != null ? otp.trim().replaceAll("\\s+", "") : null; 
    }
    
    public String getOtpType() { return otpType; }
    public void setOtpType(String otpType) { this.otpType = otpType; }
}
