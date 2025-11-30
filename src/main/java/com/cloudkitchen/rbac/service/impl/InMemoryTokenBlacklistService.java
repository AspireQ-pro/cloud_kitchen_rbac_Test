package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.service.TokenBlacklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryTokenBlacklistService.class);
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String jti, long ttlMillis) {
        long expiryTime = System.currentTimeMillis() + ttlMillis;
        blacklistedTokens.put(jti, expiryTime);
        logger.debug("Token blacklisted: {} until {}", jti, expiryTime);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null)
            return false;
        Long expiryTime = blacklistedTokens.get(jti);
        if (expiryTime == null)
            return false;

        if (expiryTime < System.currentTimeMillis()) {
            blacklistedTokens.remove(jti);
            return false;
        }
        return true;
    }

    @Scheduled(fixedRate = 3600000) // Cleanup every hour
    public void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        int initialSize = blacklistedTokens.size();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        logger.info("Cleaned up expired blacklisted tokens. Removed: {}", initialSize - blacklistedTokens.size());
    }
}
