package com.cloudkitchen.rbac.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.dto.auth.AuthRequest;
import com.cloudkitchen.rbac.dto.auth.AuthResponse;
import com.cloudkitchen.rbac.dto.auth.OtpRequest;
import com.cloudkitchen.rbac.dto.auth.OtpVerifyRequest;

import com.cloudkitchen.rbac.dto.auth.RefreshTokenRequest;
import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
    @Operation(summary = "Customer Registration", 
               description = "Register new customer only. MerchantId must be > 0 to specify which merchant the customer belongs to")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        try {
            // Only allow customer signup (merchantId > 0)
            if (req.getMerchantId() == null || req.getMerchantId() <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "Only customer signup is allowed. Valid merchantId (>0) is required"));
            }
            
            AuthResponse authResponse = auth.registerUser(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBuilder.success(201, "Customer registration successful", authResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, e.getMessage()));
        } catch (RuntimeException e) {
            // Log the actual error for debugging
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            
            if (e.getMessage().contains("already registered")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(409, e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Registration failed: " + e.getMessage()));
        } catch (Exception e) {
            // Catch any other exceptions
            System.err.println("Unexpected registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Registration failed: " + e.getMessage()));
        }
    }



    @PostMapping("/customer/login")
    public ResponseEntity<Map<String, Object>> customerLogin(@Valid @RequestBody AuthRequest req) {
        try {
            AuthResponse authResponse = auth.login(req);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Customer login successful", authResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid credentials") || e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Invalid credentials"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Login failed"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> merchantAdminLogin(@Valid @RequestBody AuthRequest req) {
        try {
            AuthResponse authResponse = auth.login(req);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Login successful", authResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid credentials") || e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Invalid credentials"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Login failed"));
        }
    }





    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        try {
            AuthResponse authResponse = auth.refresh(req);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Token refreshed successfully", authResponse));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid refresh token") || e.getMessage().contains("revoked")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Invalid or expired refresh token"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Token refresh failed"));
        }
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
    @Operation(summary = "Request OTP", 
               description = "Send OTP by phone number. Use merchantId=0 for general OTP (any user), >0 for specific merchant customers")
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody OtpRequest req) {
        try {
            // Validate merchantId is provided
            if (req.getMerchantId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "MerchantId is required (use 0 for OTP by phone number)"));
            }
            
            // Set default otpType if not provided
            if (req.getOtpType() == null || req.getOtpType().trim().isEmpty()) {
                req.setOtpType("login");
            }
            
            auth.requestOtp(req);
            String message = getSuccessMessageByType(req.getOtpType());
            return ResponseEntity.ok(ResponseBuilder.success(200, message));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(403, e.getMessage()));
            }
            if (e.getMessage().contains("Phone is blocked")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ResponseBuilder.error(429, e.getMessage()));
            }
            if (e.getMessage().contains("Too many OTP requests")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ResponseBuilder.error(429, "Rate limit exceeded: " + e.getMessage()));
            }
            if (e.getMessage().contains("Too many")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ResponseBuilder.error(429, e.getMessage()));
            }
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "User not found"));
            }
            if (e.getMessage().contains("SMS service")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ResponseBuilder.error(503, "SMS service temporarily unavailable"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "OTP request failed"));
        }
    }
    
    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", 
               description = "Verify OTP by phone number. Use merchantId=0 for general verification, >0 for specific merchant")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        try {
            // Validate merchantId is provided
            if (req.getMerchantId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "MerchantId is required (use 0 for OTP by phone number)"));
            }
            
            AuthResponse authResponse = auth.verifyOtp(req);
            if (authResponse != null) {
                String message = "password_reset".equals(req.getPurpose()) ? 
                    "OTP verified. Default password has been set. Please change it in your profile." : 
                    "OTP verified successfully";
                return ResponseEntity.ok(ResponseBuilder.success(200, message, authResponse));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "Invalid or expired OTP"));
            }
        } catch (RuntimeException e) {
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "User not found"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "OTP verification failed"));
        }
    }
    

    

    

    

    
    private String getSuccessMessageByType(String otpType) {
        switch (otpType) {
            case "password_reset":
                return "Password reset OTP sent to your phone. Valid for 5 minutes.";
            case "phone_verification":
                return "Phone verification OTP sent. Valid for 10 minutes.";
            case "account_verification":
                return "Account verification OTP sent. Valid for 15 minutes.";
            default:
                return "OTP sent to your phone successfully.";
        }
    }
}
