package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.service.OtpService;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OtpServiceImpl implements OtpService {
    
    private static final SecureRandom RANDOM = new SecureRandom();

    
    @Override
    public String generateOtp() {
        return String.format("%04d", RANDOM.nextInt(10000));
    }
    
    @Override
    public String hashOtp(String otp) {
        return otp; // Plain text storage as requested
    }
    
    @Override
    public boolean verifyOtp(String plainOtp, String storedOtp) {
        return plainOtp != null && plainOtp.equals(storedOtp);
    }
}