package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.dto.auth.*;
import com.cloudkitchen.rbac.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(auth.registerCustomer(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        return ResponseEntity.ok(auth.loginWithPassword(req));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<String> requestOtp(@RequestBody OtpRequest req) {
        auth.requestOtp(req);
        return ResponseEntity.ok("OTP sent (dummy stored in DB)");
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody OtpVerifyRequest req) {
        return ResponseEntity.ok(auth.verifyOtp(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(auth.refresh(req));
    }
    
    @PostMapping("/merchant/login")
    public ResponseEntity<AuthResponse> merchantLogin(@RequestBody MerchantLoginRequest req) {
        return ResponseEntity.ok(auth.merchantLogin(req));
    }


}
