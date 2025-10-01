package com.cloudkitchen.rbac.service;

public interface OtpService {
    String generateOtp();

    // Production-ready methods for OTP security
    // TODO: Implement these methods for production environment
    String hashOtp(String otp);
    boolean verifyOtp(String plainOtp, String hashedOtp);
}
