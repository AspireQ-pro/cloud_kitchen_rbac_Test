package com.cloudkitchen.rbac.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.cloudkitchen.rbac.service.ValidationService;
import com.cloudkitchen.rbac.util.HttpResponseUtil;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService auth;
    private final JwtTokenProvider jwt;
    private final ValidationService validationService;

    public AuthController(AuthService auth, JwtTokenProvider jwt, ValidationService validationService) {
        this.auth = auth;
        this.jwt = jwt;
        this.validationService = validationService;
    }

    @PostMapping("/signup")
    @Operation(summary = "Customer Registration",
               description = "Register new customer. MerchantId must be > 0 to specify which merchant the customer belongs to")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        try {
            log.info("Registration request received for merchantId: {}", req.getMerchantId());

            // Additional security validation
            validationService.validateRegistration(req);
            
            AuthResponse authResponse = auth.registerUser(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBuilder.success(HttpResponseUtil.CREATED, "Customer registration successful", authResponse));
        } catch (IllegalArgumentException e) {
            log.warn("Registration validation failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Registration failed");
            
            if (e.getMessage().contains("already registered")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(409, e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Registration failed"));
        } catch (Exception e) {
            log.error("Unexpected registration error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Registration failed"));
        }
    }

    @PostMapping("/customer/login")
    @Operation(summary = "Customer Login", description = "Login for customers with merchantId > 0")
    public ResponseEntity<Map<String, Object>> customerLogin(@RequestBody AuthRequest req) {
        log.info("Customer login request for merchantId: {}", req.getMerchantId());
        
        // Validate merchantId for customer login
        if (req.getMerchantId() == null || req.getMerchantId() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, "Valid merchantId (>0) is required for customer login"));
        }
        
        AuthResponse authResponse = auth.login(req);
        return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, "Customer login successful", authResponse));
    }

    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> merchantAdminLogin(@RequestBody AuthRequest req) {
/**
         * Handle login requests: enforce that only merchantId 0 (or null) is allowed, authenticate, and return the result.
         * @param {LoginRequest} req - Login request containing credentials and an optional merchantId.
         * @return {ResponseEntity<?>} - 403 FORBIDDEN if merchantId is non-null and non-zero; otherwise 200 OK with AuthResponse payload.
         */

        log.info("Login request received for merchantId: {}", req.getMerchantId());
        
        // Validate merchantId restriction for merchant login
        if (req.getMerchantId() != null && req.getMerchantId() != 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(403, "Only merchant (0) login allowed"));
        }
        
        AuthResponse authResponse = auth.login(req);
        return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, "Login successful", authResponse));
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

    @PostMapping(value = "/logout", consumes = {"application/json", "*/*"})
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // D001: Return 401 when Authorization header is missing
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Unauthorized - Authentication required."));
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Unauthorized - Authentication required."));
        }
        
        try {
            String token = authHeader.substring(7).trim();
            
            // D003 & D004: Validate token format to prevent SQL injection and XSS
            validationService.validateTokenFormat(token);
            
            // D002: Validate token properly - this will throw exception for invalid/tampered tokens
            if (!jwt.validateAccessToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Unauthorized - Authentication required."));
            }
            
            Integer userId = jwt.getUserIdFromToken(token);
            auth.logout(userId);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Logged out successfully"));
        } catch (IllegalArgumentException e) {
            // Token format validation failed (D003, D004)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, "Bad Request"));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Unauthorized - Authentication required."));
        } catch (io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.security.SignatureException | 
                 io.jsonwebtoken.UnsupportedJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Unauthorized - Authentication required."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Unauthorized - Authentication required."));
        }
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Request OTP", 
               description = "Send OTP by phone number. Use merchantId=0 for general OTP (any user), >0 for specific merchant customers")
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody OtpRequest req) {
        try {
            // Validate phone number first
            if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "Phone number is required"));
            }
            
            // Validate phone format
            validationService.validatePhone(req.getPhone());
            
            // Validate merchantId is provided
            if (req.getMerchantId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(400, "MerchantId is required (use 0 for OTP by phone number)"));
            }
            
            // Check if phone number exists in database
            if (!auth.isPhoneNumberExists(req.getPhone(), req.getMerchantId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(404, "Phone number not registered. Please register first."));
            }
            
            // Set default otpType if not provided
            if (req.getOtpType() == null || req.getOtpType().trim().isEmpty()) {
                req.setOtpType("login");
            }
            
            auth.requestOtp(req);
            String message = getSuccessMessageByType(req.getOtpType());
            return ResponseEntity.ok(ResponseBuilder.success(200, message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(400, e.getMessage()));
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
                        .body(ResponseBuilder.error(404, "Phone number not registered"));
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
               description = "Verify OTP by phone number. Use merchantId=0 for phone-based verification (any user), merchantId>0 for merchant-specific customer verification. OTP types: login, password_reset, phone_verification, account_verification")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        log.info("OTP verification request received for merchantId: {}", req.getMerchantId());

        // Optimized: Single call that validates and generates token
        AuthResponse authResponse = auth.verifyOtpAndGenerateToken(req);

        String message = "password_reset".equals(req.getOtpType()) ?
            "OTP verified. Default password has been set. Please change it in your profile." :
            "Verification successful";

        log.info("OTP verification successful for request");
        return ResponseEntity.ok(ResponseBuilder.success(200, message, authResponse));
    }
    
    private String getSuccessMessageByType(String otpType) {
        return switch (otpType) {
            case "password_reset" -> "Password reset OTP sent to your phone. Valid for 5 minutes.";
            case "phone_verification" -> "Phone verification OTP sent. Valid for 10 minutes.";
            case "account_verification" -> "Account verification OTP sent. Valid for 15 minutes.";
            default -> "OTP sent to your phone successfully.";
        };
    }
}