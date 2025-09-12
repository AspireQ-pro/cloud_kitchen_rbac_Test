package com.cloudkitchen.rbac.dto.auth;

public class OtpRequest {
    private Integer merchantId;
    private String phone;

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
