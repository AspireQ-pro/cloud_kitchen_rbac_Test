package com.cloudkitchen.rbac.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Password reset request")
public class PasswordResetRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
    @Schema(description = "User's phone number", example = "9876543210")
    private String phone;
    
    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 6, message = "OTP must be 6 digits")
    @Pattern(regexp = "\\d{6}", message = "OTP must contain only digits")
    @Schema(description = "6-digit OTP", example = "123456")
    private String otp;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    @Schema(description = "New password", example = "NewSecure123!")
    private String newPassword;
    
    @Schema(description = "Merchant ID (0 for super admin)", example = "1")
    private Integer merchantId;

    // Getters and Setters
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }
}