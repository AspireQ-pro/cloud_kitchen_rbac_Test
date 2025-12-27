package com.cloudkitchen.rbac.controller;
// Java imports
import java.util.Map;
// Spring imports
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
// DTO imports
import com.cloudkitchen.rbac.dto.auth.AuthRequest;
import com.cloudkitchen.rbac.dto.auth.OtpRequest;
import com.cloudkitchen.rbac.dto.auth.OtpVerifyRequest;
import com.cloudkitchen.rbac.dto.auth.RefreshTokenRequest;
import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.service.AuthService;
// Swagger/OpenAPI imports
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
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
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return auth.registerCustomer(req);
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
    public ResponseEntity<Map<String, Object>> customerLogin(@RequestBody AuthRequest req) {
        return auth.customerLogin(req);
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
    public ResponseEntity<Map<String, Object>> merchantAdminLogin(@RequestBody AuthRequest req) {
        return auth.merchantAdminLogin(req);
    }


    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return auth.refreshToken(req);
    }

    @PostMapping(value = "/logout", consumes = {"application/json", "*/*"})
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return auth.logoutByAuthorizationHeader(authHeader);
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
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody OtpRequest req) {
        return auth.requestOtpResponse(req);
    }
    
    /**
     * Verify provided OTP, generate authentication tokens on success, and return an appropriate HTTP response.
     *
     * @param req request containing phone, otp, merchantId, and otpType used for verification and token generation
     * @return HTTP response with success message and auth tokens (200) or an error status (400/401/404) on failure
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        return auth.verifyOtpResponse(req);
    }
}
