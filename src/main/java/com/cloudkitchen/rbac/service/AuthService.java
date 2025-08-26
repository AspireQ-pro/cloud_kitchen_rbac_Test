package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.auth.*;

public interface AuthService {
    AuthResponse registerCustomer(RegisterRequest req);
    AuthResponse loginWithPassword(AuthRequest req);
    void requestOtp(OtpRequest req);
    AuthResponse verifyOtp(OtpVerifyRequest req);
    AuthResponse refresh(RefreshTokenRequest req);
}
