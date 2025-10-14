package com.cloudkitchen.rbac.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class RateLimitService {
    
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    
    public boolean isAllowed(String clientId) {
        String key = clientId + "_" + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter());
        
        return counter.increment() <= MAX_REQUESTS_PER_MINUTE;
    }
    
    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        
        public int increment() {
            return count.incrementAndGet();
        }
    }
}