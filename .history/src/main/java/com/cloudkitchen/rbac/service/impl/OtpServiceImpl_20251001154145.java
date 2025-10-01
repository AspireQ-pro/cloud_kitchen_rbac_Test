package com.cloudkitchen.rbac.service.impl;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.cloudkitchen.rbac.service.OtpService;

@Service
public class OtpServiceImpl implements OtpService {
    private static final SecureRandom RND = new SecureRandom();

    @Override
    public String generateOtp() {
        int n = 1000 + RND.nextInt(9000);
        return String.valueOf(n);
    }

    @Override
    public String hashOtp(String otp) {
        // TODO: Implement proper OTP hashing for production
        // For development stage, return as-is with security warning
        // Production implementation should use BCrypt or Argon2 with salt
        return otp; // Plain text for development - SECURITY RISK
    }

    @Override
    public boolean verifyOtp(String plainOtp, String hashedOtp) {
        // TODO: Implement proper OTP verification for production
        // For development stage, do plain text comparison
        // Production implementation should use proper hash verification
        return plainOtp != null && plainOtp.equals(hashedOtp); // Plain text comparison for development
    }
}
