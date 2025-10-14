package com.cloudkitchen.rbac.service;

public interface OtpService {
    String generateOtp();

    // Production-ready methods for OTP security 
    String hashOtp(String otp);
    boolean verifyOtp(String plainOtp, String hashedOtp);
}
