package com.cloudkitchen.rbac.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncService {
    
    @Async("taskExecutor")
    public CompletableFuture<Void> processOtpAsync(String phone, String otp) {
        // Process OTP sending asynchronously
        return CompletableFuture.completedFuture(null);
    }
    
    @Async("taskExecutor")
    public CompletableFuture<Void> logAuditAsync(String action, String userId) {
        // Process audit logging asynchronously
        return CompletableFuture.completedFuture(null);
    }
}