package com.cloudkitchen.rbac.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    @Size(min = 100, message = "Invalid refresh token format")
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { 
        this.refreshToken = refreshToken != null ? refreshToken.trim() : null; 
    }
}
