package com.cloudkitchen.rbac.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;

import com.cloudkitchen.rbac.dto.auth.AuthRequest;
import com.cloudkitchen.rbac.dto.auth.AuthResponse;
import com.cloudkitchen.rbac.dto.auth.OtpRequest;
import com.cloudkitchen.rbac.dto.auth.OtpVerifyRequest;
import com.cloudkitchen.rbac.dto.auth.RefreshTokenRequest;
import com.cloudkitchen.rbac.dto.auth.RegisterRequest;

public interface AuthService {
    ResponseEntity<Map<String, Object>> registerCustomer(RegisterRequest req);
    AuthResponse registerUser(RegisterRequest req);
    ResponseEntity<Map<String, Object>> customerLogin(AuthRequest req);
    ResponseEntity<Map<String, Object>> merchantAdminLogin(AuthRequest req);
    AuthResponse login(AuthRequest req);

    ResponseEntity<Map<String, Object>> refreshToken(RefreshTokenRequest req);
    AuthResponse refresh(RefreshTokenRequest req);
    ResponseEntity<Map<String, Object>> logoutByAuthorizationHeader(String authHeader);
    void logout(Integer userId);

    ResponseEntity<Map<String, Object>> requestOtpResponse(OtpRequest req);
    void requestOtp(OtpRequest req);
    ResponseEntity<Map<String, Object>> verifyOtpResponse(OtpVerifyRequest req);
    AuthResponse verifyOtp(OtpVerifyRequest req);
    String verifyOtpWithStatus(OtpVerifyRequest req);

    /**
     * Optimized method that combines OTP verification and token generation in a single operation.
     * This avoids the double processing issue of calling verifyOtpWithStatus() and verifyOtp() separately.
     *
     * @param req OTP verification request
     * @return AuthResponse with tokens
     */
    AuthResponse verifyOtpAndGenerateToken(OtpVerifyRequest req);

    boolean isPhoneNumberExists(String phone, Integer merchantId);

    long getUserCount();
}
