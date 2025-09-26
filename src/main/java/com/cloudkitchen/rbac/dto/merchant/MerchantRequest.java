package com.cloudkitchen.rbac.dto.merchant;

import jakarta.validation.constraints.NotBlank;

public class MerchantRequest {
    @NotBlank(message = "Merchant name is required")
    private String merchantName;
    
    private String address;
    private String phone;
    private String email;
    
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}