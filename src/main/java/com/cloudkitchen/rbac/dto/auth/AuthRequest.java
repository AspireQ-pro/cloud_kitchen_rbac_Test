package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class AuthRequest {
    private Integer merchantId;
    
    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotBlank(message = "Password is required")
    private String password;

    // Getters and Setters
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
