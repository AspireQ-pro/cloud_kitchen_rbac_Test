package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;
import io.swagger.v3.oas.annotations.media.Schema;

public class OtpRequest {
    @Min(value = 0, message = "Merchant ID must be 0 or positive")
    @Schema(description = "Merchant ID (0 for super admin/merchant, >0 for customer)", example = "1")
    private Integer merchantId;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid phone number format")
    @Length(min = 10, max = 13, message = "Phone number must be 10-13 characters")
    @Schema(description = "User's phone number", example = "9876543210")
    private String phone;
    
    @Pattern(regexp = "^(login|password_reset|registration|phone_verification)$", 
             message = "OTP type must be one of: login, password_reset, registration, phone_verification")
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
