package com.cloudkitchen.rbac.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

public class RegisterRequest {
    
    @NotNull(message = "Merchant ID is required")
    @Min(value = 1, message = "Merchant ID must be positive")
    @JsonProperty("merchantId")
    private Integer merchantId;
    
    @NotBlank(message = "User type is required")
    @Pattern(regexp = "^(super_admin|merchant|customer)$", message = "User type must be: super_admin, merchant, or customer")
    @JsonProperty("userType")
    private String userType = "customer";
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid Indian phone number format")
    @Length(min = 10, max = 13, message = "Phone number must be 10-13 characters")
    @JsonProperty("phone")
    private String phone;
    
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must be 8+ characters with uppercase, lowercase, digit, and special character")
    @Length(min = 8, max = 128, message = "Password must be 8-128 characters")
    @JsonProperty("password")
    private String password;
    
    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']{2,50}$", message = "First name contains invalid characters")
    @Length(min = 2, max = 50, message = "First name must be 2-50 characters")
    @JsonProperty("firstName")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']{2,50}$", message = "Last name contains invalid characters")
    @Length(min = 2, max = 50, message = "Last name must be 2-50 characters")
    @JsonProperty("lastName")
    private String lastName;
    
    @Length(max = 500, message = "Address cannot exceed 500 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-,.'#/]*$", message = "Address contains invalid characters")
    @JsonProperty("address")
    private String address;
    
    @Email(message = "Invalid email format")
    @Length(max = 100, message = "Email cannot exceed 100 characters")
    @JsonProperty("email")
    private String email;

    // Getters and Setters
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        this.phone = phone != null ? phone.trim().replaceAll("\\s+", "") : null; 
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { 
        this.firstName = firstName != null ? firstName.trim() : null; 
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { 
        this.lastName = lastName != null ? lastName.trim() : null; 
    }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { 
        this.address = address != null ? address.trim() : null; 
    }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { 
        this.userType = userType != null ? userType.trim().toLowerCase() : "customer"; 
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { 
        this.email = email != null ? email.trim().toLowerCase() : null; 
    }
}
