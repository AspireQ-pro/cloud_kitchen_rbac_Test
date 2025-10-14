package com.cloudkitchen.rbac.dto.auth;

import org.hibernate.validator.constraints.Length;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class OtpRequest {
    @NotNull(message = "Merchant ID is required")
    @Min(value = 0, message = "Merchant ID must be 0 or positive")
    @Schema(description = "Merchant ID (0 for OTP by phone number, >0 for specific merchant customers)", 
            example = "0", 
            allowableValues = {"0", "1", "2", "3"})
    private Integer merchantId;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number format")
    @Length(min = 10, max = 13, message = "Phone number must be 10-13 characters")
    @Schema(description = "User's phone number", example = "9876543210")
    private String phone;
    
    @Pattern(regexp = "^(login|password_reset|registration|phone_verification|account_verification)$", 
             message = "Invalid OTP type")
    @Schema(description = "Type of OTP request", 
            example = "password_reset", 
            allowableValues = {"login", "password_reset", "registration", "phone_verification", "account_verification"})
    private String otpType; 
    
    public OtpRequest() {}
    
    public OtpRequest(String phone, Integer merchantId) {
        this.phone = phone;
        this.merchantId = merchantId;
    }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        this.phone = phone != null ? phone.trim().replaceAll("\\s+", "") : null; 
    }
    
    public String getOtpType() { return otpType; }
    public void setOtpType(String otpType) { this.otpType = otpType; }
}
