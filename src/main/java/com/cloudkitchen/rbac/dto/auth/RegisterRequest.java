package com.cloudkitchen.rbac.dto.auth;

import com.cloudkitchen.rbac.constants.ValidationMessages;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration request")
public class RegisterRequest {
    
    @Min(value = 0, message = "Merchant ID must be non-negative")
    @Schema(description = "Merchant ID (null=super_admin, 0=merchant, >0=customer)", example = "1")
    @JsonProperty("merchantId")
    private Integer merchantId;
    
    @Schema(description = "Phone number", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ValidationMessages.PHONE_REQUIRED)
    @Pattern(regexp = "^[6-9]\\d{9}$", message = ValidationMessages.PHONE_FORMAT)
    @JsonProperty("phone")
    private String phone;
    
    @Schema(description = "Password", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$", 
             message = ValidationMessages.PASSWORD_FORMAT)
    @JsonProperty("password")
    private String password;
    
    @Schema(description = "First name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ValidationMessages.FIRST_NAME_REQUIRED)
    @Pattern(regexp = "^[a-zA-Z\\s\\-']{2,50}$", message = ValidationMessages.NAME_FORMAT)
    @JsonProperty("firstName")
    private String firstName;
    
    @Schema(description = "Last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = ValidationMessages.LAST_NAME_REQUIRED)
    @Pattern(regexp = "^[a-zA-Z\\s\\-']{2,50}$", message = ValidationMessages.NAME_FORMAT)
    @JsonProperty("lastName")
    private String lastName;
    
    @Schema(description = "Address", example = "123 Main Street")
    @Size(max = 500, message = ValidationMessages.ADDRESS_SIZE)
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-,.'#/]*$", message = ValidationMessages.ADDRESS_FORMAT)
    @JsonProperty("address")
    private String address;
    
    @Schema(description = "Email", example = "john@example.com")
    @Email(message = ValidationMessages.EMAIL_FORMAT)
    @Size(max = 100, message = ValidationMessages.EMAIL_SIZE)
    @JsonProperty("email")
    private String email;

    public RegisterRequest() {}

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = sanitizePhone(phone); }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = sanitizeName(firstName); }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = sanitizeName(lastName); }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = sanitizeAddress(address); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = sanitizeEmail(email); }

    private String sanitizePhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.trim().replaceAll("[^0-9]", "");
        return cleaned.length() == 10 ? cleaned : phone.trim();
    }

    private String sanitizeName(String name) {
        if (name == null) return null;
        String cleaned = name.trim().replaceAll("[^a-zA-Z\\s\\-']", "");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
    }

    private String sanitizeAddress(String address) {
        if (address == null) return null;
        String cleaned = address.trim().replaceAll("[<>\"'&]", "");
        return cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
    }

    private String sanitizeEmail(String email) {
        return email != null ? email.trim().toLowerCase(java.util.Locale.ROOT) : null;
    }
}