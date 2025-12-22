package com.cloudkitchen.rbac.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration request")
@JsonIgnoreProperties(ignoreUnknown = false)
public class RegisterRequest {
    
    @Min(value = 0, message = "Merchant ID must be non-negative")
    @Schema(description = "Merchant ID (null=super_admin, 0=merchant, >0=customer)", example = "1")
    @JsonProperty("merchantId")
    private Integer merchantId;
    
    @Schema(description = "Phone number", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number")
    @JsonProperty("phone")
    private String phone;
    
    @Schema(description = "Password", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @JsonProperty("password")
    private String password;
    
    @Schema(description = "First name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "First name is required")
    @JsonProperty("firstName")
    private String firstName;
    
    @Schema(description = "Last name", example = "Doe")
    @JsonProperty("lastName")
    private String lastName;
    
    @Schema(description = "Address", example = "123 Main Street")
    @JsonProperty("address")
    private String address;

    public RegisterRequest() {}

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        this.phone = phone != null ? phone.trim().replaceAll("[^0-9]", "") : null; 
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
    public void setAddress(String address) { this.address = address; }
}