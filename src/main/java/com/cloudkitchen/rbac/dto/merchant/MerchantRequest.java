package com.cloudkitchen.rbac.dto.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.cloudkitchen.rbac.constants.ResponseMessages;

@Schema(description = "Merchant registration request")
public class MerchantRequest {
    @Schema(description = "Merchant business name", example = "Yogesh Kitchen", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ResponseMessages.Validation.MERCHANT_NAME_WHITESPACE)
    @Size(max = 100, message = "Merchant name must not exceed 100 characters")
    private String merchantName;

    @Schema(description = "Merchant email address", example = "yogesh@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ResponseMessages.Validation.EMAIL_WHITESPACE)
    @Email(message = ResponseMessages.Validation.INVALID_EMAIL)
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Schema(description = "GST Identification Number (15 characters)", example = "29ABCDE1234F1Z5")
    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = ResponseMessages.Validation.INVALID_FORMAT)
    @Size(max = 15, message = "GSTIN must be exactly 15 characters")
    private String gstin;

    @Schema(description = "Username for merchant login", example = "yogesh_kitchen", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ResponseMessages.Validation.USERNAME_WHITESPACE)
    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String username;

    @Schema(description = "Strong password with uppercase, lowercase, digit and special character", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ResponseMessages.Validation.REQUIRED_FIELD_MISSING)
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,100}$", 
             message = ResponseMessages.Validation.PASSWORD_TOO_WEAK)
    private String password;

    @Schema(description = "10-digit Indian mobile number", example = "8095242733", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ResponseMessages.Validation.REQUIRED_FIELD_MISSING)
    @Pattern(regexp = "^[6-9]\\d{9}$", message = ResponseMessages.Validation.INVALID_PHONE)
    private String phone;

    @Schema(description = "Complete business address", example = "123 Main Street, Mumbai, Maharashtra 400001")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Schema(description = "FSSAI License Number (14 digits)", example = "12345678901234")
    @Size(max = 50, message = "FSSAI license number must not exceed 50 characters")
    private String fssaiLicense;

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Getters and Setters with automatic trimming
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { 
        this.merchantName = normalize(merchantName); 
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { 
        this.email = normalize(email); 
    }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { 
        this.gstin = normalize(gstin); 
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { 
        this.username = normalize(username); 
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { 
        this.password = normalize(password); 
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        this.phone = normalize(phone); 
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { 
        this.address = normalize(address); 
    }

    public String getFssaiLicense() { return fssaiLicense; }
    public void setFssaiLicense(String fssaiLicense) { 
        this.fssaiLicense = normalize(fssaiLicense); 
    }
}
