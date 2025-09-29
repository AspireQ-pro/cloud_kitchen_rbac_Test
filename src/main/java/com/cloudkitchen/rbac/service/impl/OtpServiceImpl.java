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
}
