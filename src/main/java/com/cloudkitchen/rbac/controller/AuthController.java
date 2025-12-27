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

import com.cloudkitchen.rbac.constants.ResponseMessages;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    /**
     * Handle customer registration: validate input, register a user for the specified merchant,
     * and return authentication tokens on success.
     *
     * @param req registration request body containing name, email, phone, password, and merchantId;
     *            all fields required and merchantId must be > 0
     * @return HTTP response with success payload (201) including access/refresh tokens and user info,
     *         or error payloads with status codes 400 (invalid/missing data), 404 (merchant not found),
     *         409 (conflict/user exists), or 500 (server error)
     */
    @PostMapping("/signup")
    @Operation(
        summary = "Customer Registration",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Request Body:** JSON with name, email, phone, password, merchantId\n" +
                     "2. **MerchantId:** Must be > 0 to specify which merchant the customer belongs to\n" +
                     "3. **Validation:** All fields are required and must be valid\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid registration with all fields\n" +
                     "- Invalid merchantId (404)\n" +
                     "- Duplicate email/phone (409)\n" +
                     "- Invalid email format (400)\n" +
                     "- Missing required fields (400)"
    )
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        try {
            log.info("Registration request received for merchantId: {}", req.getMerchantId());

            // Additional security validation
            validationService.validateRegistration(req);
            
            AuthResponse authResponse = auth.registerUser(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBuilder.success(HttpResponseUtil.CREATED, ResponseMessages.Auth.REGISTRATION_SUCCESS, authResponse));
        } catch (IllegalArgumentException e) {
            log.warn("Registration validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            
            // Handle specific merchant not found error
            if (e.getMessage().contains("Merchant with ID") && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, e.getMessage()));
            }
            
            // Handle user already exists error
            if (e.getMessage().contains("already registered")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(HttpResponseUtil.CONFLICT, e.getMessage()));
            }
            
            // Handle phone already exists error
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ResponseBuilder.error(HttpResponseUtil.CONFLICT, e.getMessage()));
            }
            
            // Handle validation errors
            if (e.getMessage().contains("Invalid") || e.getMessage().contains("required")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, e.getMessage()));
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(HttpResponseUtil.INTERNAL_SERVER_ERROR, "Registration failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected registration error occurred: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(HttpResponseUtil.INTERNAL_SERVER_ERROR, "Registration failed due to unexpected error"));
        }
    }

    /**
     * Authenticate a customer for a given merchant and return authentication tokens and user info.
     *
     * @param req request body containing email, password, and merchantId
     *            (merchantId must be > 0 for customer login)
     * @return HTTP response: 200 with auth data (accessToken, refreshToken, user) on success;
     *         400 for missing/invalid merchantId; 401 for invalid credentials
     */
    @PostMapping("/customer/login")
    @Operation(
        summary = "Customer Login",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Request Body:** JSON with email, password, merchantId\n" +
                     "2. **MerchantId:** Must be > 0 for customer login\n" +
                     "3. **Authentication:** Valid customer credentials required\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid customer credentials with merchantId > 0\n" +
                     "- Invalid credentials (401)\n" +
                     "- Missing merchantId (400)\n" +
                     "- MerchantId = 0 or null (400)\n" +
                     "- Non-existent customer (401)"
    )
    public ResponseEntity<Map<String, Object>> customerLogin(@RequestBody AuthRequest req) {
        log.info("Customer login request for merchantId: {}", req.getMerchantId());
        
        // Validate merchantId for customer login
        if (req.getMerchantId() == null || req.getMerchantId() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, ResponseMessages.Auth.CUSTOMER_MERCHANT_ID_REQUIRED));
        }
        
        AuthResponse authResponse = auth.login(req);
        return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Auth.CUSTOMER_LOGIN_SUCCESS, authResponse));
    }

    /**
     * Authenticate merchant or admin users; enforce merchantId = null or 0 for merchant login and
     * return authentication result.
     *
     * @param req login request containing email, password, and optional merchantId
     * @return 403 FORBIDDEN if merchantId is non-null and non-zero; 401 for invalid credentials;
     *         200 OK with auth payload (accessToken, refreshToken, user) on success
     */
    @PostMapping(value = "/login", consumes = "application/json")
    @Operation(
        summary = "Merchant/Admin Login",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Request Body:** JSON with email, password\n" +
                     "2. **MerchantId:** Must be 0 or null for merchant/admin login\n" +
                     "3. **Authentication:** Valid merchant/admin credentials required\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid merchant credentials with merchantId = 0\n" +
                     "- Valid admin credentials\n" +
                     "- Invalid credentials (401)\n" +
                     "- MerchantId > 0 (403)\n" +
                     "- Missing credentials (400)"
    )
    public ResponseEntity<Map<String, Object>> merchantAdminLogin(@RequestBody AuthRequest req) {
        log.info("Login request received for merchantId: {}", req.getMerchantId());
        
        // Validate merchantId restriction for merchant login
        if (req.getMerchantId() != null && req.getMerchantId() != 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Auth.MERCHANT_LOGIN_ONLY));
        }
        AuthResponse authResponse = auth.login(req);
        return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Auth.LOGIN_SUCCESS, authResponse));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh Token",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Request Body:** JSON with refreshToken\n" +
                     "2. **Token:** Valid refresh token required\n" +
                     "3. **Response:** New access and refresh tokens\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid refresh token\n" +
                     "- Invalid refresh token (401)\n" +
                     "- Expired refresh token (401)\n" +
                     "- Revoked refresh token (401)\n" +
                     "- Missing refresh token (400)"
    )
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        try {
            AuthResponse authResponse = auth.refresh(req);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Auth.TOKEN_REFRESH_SUCCESS, authResponse));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Invalid refresh token") || e.getMessage().contains("revoked")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(HttpResponseUtil.UNAUTHORIZED, "Invalid or expired refresh token"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(HttpResponseUtil.INTERNAL_SERVER_ERROR, "Token refresh failed"));
        }
    }

    @PostMapping(value = "/logout", consumes = {"application/json", "*/*"})
    @Operation(
        summary = "Logout",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Authentication:** Bearer token in Authorization header\n" +
                     "2. **Header:** Authorization: Bearer {your_jwt_token}\n" +
                     "3. **Action:** Invalidates current session and tokens\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid JWT token\n" +
                     "- Invalid JWT token (401)\n" +
                     "- Expired JWT token (401)\n" +
                     "- Missing Authorization header (401)\n" +
                     "- Malformed token (401)"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // D001: Return 401 when Authorization header is missing
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(HttpResponseUtil.UNAUTHORIZED, ResponseMessages.Auth.AUTHENTICATION_REQUIRED));
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(HttpResponseUtil.UNAUTHORIZED, ResponseMessages.Auth.AUTHENTICATION_REQUIRED));
        }
        
        try {
            String token = authHeader.substring(7).trim();
            
            // D003 & D004: Validate token format to prevent SQL injection and XSS
            validationService.validateTokenFormat(token);
            
            // D002: Validate token properly - this will throw exception for invalid/tampered tokens
            if (!jwt.validateAccessToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(HttpResponseUtil.UNAUTHORIZED, ResponseMessages.Auth.AUTHENTICATION_REQUIRED));
            }
            
            Integer userId = jwt.getUserIdFromToken(token);
            auth.logout(userId);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Auth.LOGOUT_SUCCESS));
        } catch (IllegalArgumentException e) {
            // Token format validation failed (D003, D004)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, "Bad Request"));
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(HttpResponseUtil.UNAUTHORIZED, ResponseMessages.Auth.AUTHENTICATION_REQUIRED));
        } catch (io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.security.SignatureException | 
                 io.jsonwebtoken.UnsupportedJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(HttpResponseUtil.UNAUTHORIZED, ResponseMessages.Auth.AUTHENTICATION_REQUIRED));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(HttpResponseUtil.UNAUTHORIZED, ResponseMessages.Auth.AUTHENTICATION_REQUIRED));
        }
    }

    /**
     * Handle OTP request for a phone number and merchant, performing validation, rate-limiting,
     * and SMS service handling.
     *
     * @param req request body containing phone, merchantId, and optional otpType (defaults to "login")
     * @return HTTP response containing status and message; 200 on success,
     *         400/404/403/429/503/500 on errors
     */
    @PostMapping("/otp/request")
    @Operation(
        summary = "Request OTP",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Request Body:** JSON with phone, merchantId, otpType\n" +
                     "2. **MerchantId:** 0 for general OTP, >0 for specific merchant customers\n" +
                     "3. **OTP Types:** login, password_reset, phone_verification, account_verification\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid phone number with merchantId\n" +
                     "- Invalid phone format (400)\n" +
                     "- Phone not registered (404)\n" +
                     "- Too many requests (429)\n" +
                     "- Missing merchantId (400)"
    )
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody OtpRequest req) {
        try {
            // Validate phone number first
            if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, ResponseMessages.Otp.PHONE_REQUIRED));
            }
            
            // Validate phone format
            validationService.validatePhone(req.getPhone());
            
            // Validate merchantId is provided
            if (req.getMerchantId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, ResponseMessages.Otp.MERCHANT_ID_REQUIRED_OTP));
            }
            
            // Check if phone number exists in database
            if (!auth.isPhoneNumberExists(req.getPhone(), req.getMerchantId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Otp.PHONE_NOT_REGISTERED));
            }
            
            // Set default otpType if not provided
            if (req.getOtpType() == null || req.getOtpType().trim().isEmpty()) {
                req.setOtpType("login");
            }
            
            auth.requestOtp(req);
            String message = getSuccessMessageByType(req.getOtpType());
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, e.getMessage()));
            }
            if (e.getMessage().contains("Phone is blocked")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ResponseBuilder.error(HttpResponseUtil.TOO_MANY_REQUESTS, e.getMessage()));
            }
            if (e.getMessage().contains("Too many OTP requests")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ResponseBuilder.error(HttpResponseUtil.TOO_MANY_REQUESTS, "Rate limit exceeded: " + e.getMessage()));
            }
            if (e.getMessage().contains("Too many")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ResponseBuilder.error(HttpResponseUtil.TOO_MANY_REQUESTS, e.getMessage()));
            }
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Otp.PHONE_NOT_REGISTERED));
            }
            if (e.getMessage().contains("SMS service")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ResponseBuilder.error(HttpResponseUtil.SERVICE_UNAVAILABLE, ResponseMessages.Otp.SMS_SERVICE_UNAVAILABLE));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(HttpResponseUtil.INTERNAL_SERVER_ERROR, ResponseMessages.Otp.OTP_REQUEST_FAILED));
        }
    }
    
    /**
     * Verify provided OTP, generate authentication tokens on success, and return an appropriate HTTP response.
     *
     * @param req request containing phone, otp, merchantId, and otpType used for verification and token generation
     * @return HTTP response with success message and auth tokens (200) or an error status (400/401/404) on failure
     */
    @PostMapping("/otp/verify")
    @Operation(
        summary = "Verify OTP",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **Request Body:** JSON with phone, otp, merchantId, otpType\n" +
                     "2. **MerchantId:** 0 for phone-based verification, >0 for merchant-specific\n" +
                     "3. **OTP Types:** login, password_reset, phone_verification, account_verification\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Valid OTP with correct phone/merchantId\n" +
                     "- Invalid OTP code (401)\n" +
                     "- Expired OTP (401)\n" +
                     "- Wrong phone number (404)\n" +
                     "- Missing required fields (400)"
    )
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        log.info("OTP verification request received for merchantId: {}", req.getMerchantId());

        // Optimized: Single call that validates and generates token
        AuthResponse authResponse = auth.verifyOtpAndGenerateToken(req);

        String message = ResponseMessages.Otp.OTP_TYPE_PASSWORD_RESET.equals(req.getOtpType())
                ? ResponseMessages.Otp.OTP_PASSWORD_RESET_SUCCESS
                : ResponseMessages.Otp.OTP_VERIFIED;

        log.info("OTP verified for phone={}, merchantId={}, otpType={}", req.getPhone(), req.getMerchantId(), req.getOtpType());
        return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, message, authResponse));
    }
    
    private String getSuccessMessageByType(String otpType) {
        return switch (otpType) {
            case ResponseMessages.Otp.OTP_TYPE_PASSWORD_RESET -> ResponseMessages.Otp.PASSWORD_RESET_OTP;
            case "phone_verification" -> ResponseMessages.Otp.PHONE_VERIFICATION_OTP;
            case "account_verification" -> ResponseMessages.Otp.ACCOUNT_VERIFICATION_OTP;
            default -> ResponseMessages.Otp.OTP_SENT;
        };
    }
}
