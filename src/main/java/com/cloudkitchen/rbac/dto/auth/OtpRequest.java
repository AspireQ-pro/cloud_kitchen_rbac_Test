package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class OtpRequest {
    private Integer merchantId;
    
    @NotBlank
    private String phone;
    
    public OtpRequest() {}
    
    public OtpRequest(String phone, Integer merchantId) {
        this.phone = phone;
        this.merchantId = merchantId;
    }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
