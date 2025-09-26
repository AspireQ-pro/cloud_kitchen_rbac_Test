package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.*;

public class RegisterRequest {
    private Integer merchantId; // Optional for super_admin and merchant users
    
    @NotBlank
    @Pattern(regexp = "^(super_admin|merchant|customer)$", message = "Invalid user type")
    private String userType = "customer"; // Can be: super_admin, merchant, customer
    
    @NotBlank(message = "Mobile number is required")
    private String phone;
    
    @NotBlank
    @Size(min = 8, max = 50)
    private String password;
    
    @NotBlank
    @Size(min = 2, max = 50)
    private String firstName;
    
    @NotBlank
    @Size(min = 2, max = 50)
    private String lastName;
    
    private String address;

    // Getters and Setters
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}
