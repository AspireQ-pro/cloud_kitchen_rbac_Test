package com.cloudkitchen.rbac.dto.auth;

import org.hibernate.validator.constraints.Length;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Reset password using a reset token")
public class PasswordResetRequest {
    @Schema(description = "Password reset token issued after OTP verification", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Reset token is required")
    private String resetToken;

    @Schema(description = "New password", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "New password is required")
    @Length(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String newPassword;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken != null ? resetToken.trim() : null;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
