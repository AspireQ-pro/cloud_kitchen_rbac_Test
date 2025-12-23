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
    @NotBlank(message = ResponseMessages.Validation.REQUIRED_FIELD_MISSING)
    @Size(max = 100, message = "Merchant name must not exceed 100 characters")
    private String merchantName;

    @Schema(description = "Merchant email address", example = "yogesh@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ResponseMessages.Validation.REQUIRED_FIELD_MISSING)
    @Email(message = ResponseMessages.Validation.INVALID_EMAIL)
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Schema(description = "GST Identification Number (15 characters)", example = "29ABCDE1234F1Z5")
    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = ResponseMessages.Validation.INVALID_FORMAT)
    @Size(max = 15, message = "GSTIN must be exactly 15 characters")
    private String gstin;

    @Schema(description = "Username for merchant login", example = "yogesh_kitchen", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ResponseMessages.Validation.REQUIRED_FIELD_MISSING)
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

    // Utility method to trim and validate string fields
    private String trimAndValidate(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        
        String trimmed = value.trim();
        
        // Check if the field becomes empty after trimming for required fields
        if (trimmed.isEmpty() && isRequiredField(fieldName)) {
            throw new IllegalArgumentException(getValidationMessage(fieldName));
        }
        
        // Additional validation for specific fields
        if (!trimmed.isEmpty()) {
            validateField(trimmed, fieldName);
        }
        
        return trimmed.isEmpty() ? null : trimmed;
    }
    
    private void validateField(String value, String fieldName) {
        switch (fieldName) {
            case "phone" -> {
                if (value.length() != 10) {
                    throw new IllegalArgumentException(ResponseMessages.Validation.INVALID_PHONE);
                }
                if (!value.matches("^[6-9]\\d{9}$")) {
                    throw new IllegalArgumentException(ResponseMessages.Validation.INVALID_PHONE);
                }
            }
            case "password" -> {
                if (value.length() > 100) {
                    throw new IllegalArgumentException(ResponseMessages.Validation.PASSWORD_TOO_WEAK);
                }
                if (value.length() < 8) {
                    throw new IllegalArgumentException(ResponseMessages.Validation.PASSWORD_TOO_WEAK);
                }
                if (!value.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,100}$")) {
                    throw new IllegalArgumentException(ResponseMessages.Validation.PASSWORD_TOO_WEAK);
                }
            }
            case "email" -> {
                if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    throw new IllegalArgumentException(ResponseMessages.Validation.INVALID_EMAIL);
                }
            }
            case "gstin" -> {
                if (!value.isEmpty() && !value.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) {
                    throw new IllegalArgumentException(ResponseMessages.Validation.INVALID_FORMAT);
                }
            }
        }
    }
    
    private boolean isRequiredField(String fieldName) {
        return "merchantName".equals(fieldName) || "email".equals(fieldName) || 
               "username".equals(fieldName) || "password".equals(fieldName) || 
               "phone".equals(fieldName);
    }
    
    private String getValidationMessage(String fieldName) {
        return switch (fieldName) {
            case "merchantName" -> ResponseMessages.Validation.MERCHANT_NAME_WHITESPACE;
            case "username" -> ResponseMessages.Validation.USERNAME_WHITESPACE;
            case "email" -> ResponseMessages.Validation.EMAIL_WHITESPACE;
            case "password" -> ResponseMessages.Validation.REQUIRED_FIELD_MISSING;
            case "phone" -> ResponseMessages.Validation.REQUIRED_FIELD_MISSING;
            default -> ResponseMessages.Validation.FIELD_CANNOT_BE_EMPTY;
        };
    }

    // Getters and Setters with automatic trimming
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { 
        this.merchantName = trimAndValidate(merchantName, "merchantName"); 
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { 
        this.email = trimAndValidate(email, "email"); 
    }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { 
        this.gstin = trimAndValidate(gstin, "gstin"); 
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { 
        this.username = trimAndValidate(username, "username"); 
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { 
        this.password = trimAndValidate(password, "password"); 
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { 
        this.phone = trimAndValidate(phone, "phone"); 
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { 
        this.address = trimAndValidate(address, "address"); 
    }

    public String getFssaiLicense() { return fssaiLicense; }
    public void setFssaiLicense(String fssaiLicense) { 
        this.fssaiLicense = trimAndValidate(fssaiLicense, "fssaiLicense"); 
    }
}