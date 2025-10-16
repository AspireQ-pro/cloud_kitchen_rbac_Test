package com.cloudkitchen.rbac.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

@Schema(description = "Login request for merchants, customers, and admins")
public class AuthRequest {
    @Schema(description = "Merchant ID (0=merchant/admin login, >0=customer login)", example = "0", required = true)
    @NotNull(message = "MerchantId is required")
    @Min(value = 0, message = "Merchant ID must be 0 or positive")
    private Integer merchantId;
    
    @Schema(description = "Username for merchant/admin (merchantId=0) or phone for customer (merchantId>0)", example = "yogesh_kitchen", required = true)
    @NotBlank(message = "Username is required")
    private String username;
    
    @Schema(description = "Password for login", example = "SecurePass123!", required = true)
    @NotBlank(message = "Password is required")
    @Length(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String password;

    // Getters and Setters
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { 
        this.username = username != null ? username.trim() : null; 
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
