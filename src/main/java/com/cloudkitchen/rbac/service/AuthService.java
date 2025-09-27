package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.auth.*;

public interface AuthService {
    AuthResponse registerUser(RegisterRequest req);
    AuthResponse login(AuthRequest req);

    AuthResponse refresh(RefreshTokenRequest req);
    void logout(Integer userId);

    void requestOtp(OtpRequest req);
    boolean verifyOtp(OtpVerifyRequest req);
    void resetPassword(PasswordResetRequest req);
}
