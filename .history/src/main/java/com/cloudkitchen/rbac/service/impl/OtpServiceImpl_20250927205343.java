package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.service.OtpService;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OtpServiceImpl implements OtpService {
    private static final SecureRandom RND = new SecureRandom();

    @Override
    public String generateOtp() {
        int n = 1000 + RND.nextInt(900000);
        return String.valueOf(n);
    }
}
