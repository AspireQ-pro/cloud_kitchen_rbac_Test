package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

public class AuthRequest {
    @Min(value = 0, message = "Merchant ID must be 0 or positive")
    private Integer merchantId;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid phone number format")
    @Length(min = 10, max = 13, message = "Phone number must be 10-13 characters")
    private String phone;
    
    @NotBlank(message = "Password is required")
    @Length(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String password;

    // Getters and Setters
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        this.phone = phone != null ? phone.trim().replaceAll("\\s+", "") : null; 
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
