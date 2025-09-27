package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.dto.auth.*;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {
    private final AuthService auth;
    private final JwtTokenProvider jwt;

    public AuthController(AuthService auth, JwtTokenProvider jwt) {
        this.auth = auth;
        this.jwt = jwt;
    }

    @PostMapping("/signup")
    @Operation(summary = "User Registration", 
               description = "Register new users. User type is automatically determined:\n" +
                           "• **merchantId = 0**: Creates super_admin user\n" +
                           "• **merchantId > 0**: Creates customer user\n\n" +
                           "No need to specify userType - it's determined automatically based on merchantId.")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse authResponse = auth.registerUser(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseBuilder.success(201, "User created successfully", authResponse));
    }



    @PostMapping("/customer/login")
    public ResponseEntity<Map<String, Object>> customerLogin(@Valid @RequestBody AuthRequest req) {
        AuthResponse authResponse = auth.login(req);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Customer login successful", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> merchantAdminLogin(@Valid @RequestBody AuthRequest req) {
        AuthResponse authResponse = auth.login(req);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Login successful", authResponse));
    }





    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        AuthResponse authResponse = auth.refresh(req);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Token refreshed successfully", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, "Invalid Authorization header"));
        }
        try {
            String token = authHeader.substring(7);
            Integer userId = jwt.getUserIdFromToken(token);
            auth.logout(userId);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Invalid token"));
        }
    }
    

    
    @PostMapping("/otp/request")
    @Operation(summary = "Request OTP", description = "Send OTP for user verification - handles all 3 roles (super_admin, merchant, customer)")
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody OtpRequest req) {
        auth.requestOtp(req);
        return ResponseEntity.ok(ResponseBuilder.success(200, "OTP sent successfully"));
    }
    
    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", description = "Verify OTP for user authentication - returns true/false")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        boolean isValid = auth.verifyOtp(req);
        if (isValid) {
            return ResponseEntity.ok(ResponseBuilder.success(200, "OTP verified successfully", Map.of("verified", true)));
        } else {
            return ResponseEntity.ok(ResponseBuilder.success(200, "OTP verification failed", Map.of("verified", false)));
        }
    }
    
    @PostMapping("/password/reset")
    @Operation(summary = "Reset Password", description = "Reset password using OTP verification")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
        auth.resetPassword(req);
        return ResponseEntity.ok(ResponseBuilder.success(200, "Password reset successfully"));
    }


}
