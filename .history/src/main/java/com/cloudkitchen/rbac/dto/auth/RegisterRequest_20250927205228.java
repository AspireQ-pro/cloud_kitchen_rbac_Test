package com.cloudkitchen.rbac.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;
import io.swagger.v3.oas.annotations.media.Schema;

public class RegisterRequest {
    
    @Schema(description = "Merchant ID (use 0 for super_admin, >0 for merchant/customer)", example = "1")
    @Min(value = 0, message = "Merchant ID must be min ")
    @JsonProperty("merchantId")
    private Integer merchantId = 1; // Default to 1 for customer
    

    
    @Schema(description = "Phone number (10 digits starting with 6-9)", example = "9876543210", required = true)
    @NotNull(message = "Phone number cannot be null")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number format. Must be 10 digits starting with 6-9")
    @Length(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
    @JsonProperty("phone")
    private String phone;
    
    @Schema(description = "Password (8-128 chars with uppercase, lowercase, digit, special char)", example = "SecurePass123!", required = true)
    @NotNull(message = "Password cannot be null")
    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$", 
             message = "Password must be 8-128 characters with uppercase, lowercase, digit, and special character")
    @Length(min = 8, max = 128, message = "Password must be 8-128 characters")
    @JsonProperty("password")
    private String password;
    
    @Schema(description = "First name", example = "John", required = true)
    @NotNull(message = "First name cannot be null")
    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']{2,50}$", message = "First name contains invalid characters")
    @Length(min = 2, max = 50, message = "First name must be 2-50 characters")
    @JsonProperty("firstName")
    private String firstName;
    
    @Schema(description = "Last name", example = "Doe", required = true)
    @NotNull(message = "Last name cannot be null")
    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']{2,50}$", message = "Last name contains invalid characters")
    @Length(min = 2, max = 50, message = "Last name must be 2-50 characters")
    @JsonProperty("lastName")
    private String lastName;
    
    @Schema(description = "Address", example = "123 Main Street, City, State")
    @Length(max = 500, message = "Address cannot exceed 500 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-,.'#/]*$", message = "Address contains invalid characters")
    @JsonProperty("address")
    private String address;
    
    @Schema(description = "Email address", example = "john.doe@example.com")
    @Email(message = "Invalid email format")
    @Length(max = 100, message = "Email cannot exceed 100 characters")
    @JsonProperty("email")
    private String email;

    // Getters and Setters
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        if (phone != null) {
            // Remove all non-digit characters except +
            String cleaned = phone.trim().replaceAll("[^0-9+]", "");
            // Remove + prefix if present
            if (cleaned.startsWith("+91")) {
                cleaned = cleaned.substring(3);
            } else if (cleaned.startsWith("+")) {
                cleaned = cleaned.substring(1);
            }
            this.phone = cleaned;
        } else {
            this.phone = null;
        }
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { 
        if (firstName != null) {
            String cleaned = firstName.trim().replaceAll("[^a-zA-Z\\s\\-']", "");
            this.firstName = cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
        } else {
            this.firstName = null;
        }
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { 
        if (lastName != null) {
            String cleaned = lastName.trim().replaceAll("[^a-zA-Z\\s\\-']", "");
            this.lastName = cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
        } else {
            this.lastName = null;
        }
    }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { 
        if (address != null) {
            String cleaned = address.trim().replaceAll("[<>\"'&]", "");
            this.address = cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
        } else {
            this.address = null;
        }
    }



    public String getEmail() { return email; }
    public void setEmail(String email) { 
        this.email = email != null ? email.trim().toLowerCase() : null; 
    }
}
