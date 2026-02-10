package com.cloudkitchen.rbac.dto.auth;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Password reset token response")
public class PasswordResetTokenResponse {
    @Schema(description = "Password reset token to be used for setting a new password")
    private String resetToken;

    @Schema(description = "Token expiration time")
    private LocalDateTime expiresAt;

    public PasswordResetTokenResponse() {
    }

    public PasswordResetTokenResponse(String resetToken, LocalDateTime expiresAt) {
        this.resetToken = resetToken;
        this.expiresAt = expiresAt;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
