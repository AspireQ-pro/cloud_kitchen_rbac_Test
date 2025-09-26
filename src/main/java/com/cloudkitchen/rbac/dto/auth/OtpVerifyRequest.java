package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

public class OtpVerifyRequest {
    @Min(value = 0, message = "Merchant ID must be 0 or positive")
    private Integer merchantId;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid phone number format")
    @Length(min = 10, max = 13, message = "Phone number must be 10-13 characters")
    private String phone;
    
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{4}$", message = "OTP must be 4 digits")
    @Length(min = 4, max = 4, message = "OTP must be exactly 4 digits")
    private String otp;

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
}
