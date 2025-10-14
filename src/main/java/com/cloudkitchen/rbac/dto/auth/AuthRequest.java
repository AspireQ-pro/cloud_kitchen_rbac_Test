package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

public class AuthRequest {
    @NotNull(message = "MerchantId is required")
    @Min(value = 0, message = "Merchant ID must be 0 or positive")
    private Integer merchantId;
    
    // For merchantId=0: username (merchant/super admin)
    // For merchantId>0: phone (customer)
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Length(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String password;

    // Getters and Setters
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { 
        this.username = username != null ? username.trim() : null; 
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
