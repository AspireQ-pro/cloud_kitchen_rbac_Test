package com.cloudkitchen.rbac.dto.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Merchant registration request")
public class MerchantRequest {
    @Schema(description = "Merchant business name", example = "Yogesh Kitchen", required = true)
    @NotBlank(message = "Merchant name is required")
    @Size(max = 100, message = "Merchant name must not exceed 100 characters")
    private String merchantName;

    @Schema(description = "Merchant email address", example = "yogesh@gmail.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Schema(description = "GST Identification Number (15 characters)", example = "29ABCDE1234F1Z5")
    @Size(max = 20, message = "GSTIN must not exceed 20 characters")
    private String gstin;

    @Schema(description = "Username for merchant login", example = "yogesh_kitchen", required = true)
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String username;

    @Schema(description = "Strong password with uppercase, lowercase, digit and special character", example = "SecurePass123!", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    private String password;

    @Schema(description = "10-digit Indian mobile number", example = "8095242733", required = true)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit Indian mobile number")
    private String phone;

    @Schema(description = "Complete business address", example = "123 Main Street, Mumbai, Maharashtra 400001")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Schema(description = "FSSAI License Number (14 digits)", example = "12345678901234")
    @Size(max = 50, message = "FSSAI license number must not exceed 50 characters")
    private String fssaiLicense;

    // Getters and Setters
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getFssaiLicense() { return fssaiLicense; }
    public void setFssaiLicense(String fssaiLicense) { this.fssaiLicense = fssaiLicense; }
}