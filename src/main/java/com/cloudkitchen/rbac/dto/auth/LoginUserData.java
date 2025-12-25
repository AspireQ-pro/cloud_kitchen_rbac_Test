package com.cloudkitchen.rbac.dto.auth;

public class LoginUserData {
    private Integer userId;
    private String phone;
    private String passwordHash;
    private String userType;
    private Integer merchantId;
    private Integer customerId;
    private String roles;
    private String permissions;
    private Boolean active;

    public LoginUserData() {}

    public LoginUserData(Integer userId, String phone, String passwordHash, String userType, 
                        Integer merchantId, Integer customerId, String roles, String permissions, Boolean active) {
        this.userId = userId;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.merchantId = merchantId;
        this.customerId = customerId;
        this.roles = roles;
        this.permissions = permissions;
        this.active = active;
    }

    // Getters and setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}