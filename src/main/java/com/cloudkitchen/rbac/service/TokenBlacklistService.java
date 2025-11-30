package com.cloudkitchen.rbac.service;

public interface TokenBlacklistService {
    void blacklist(String jti, long ttlMillis);

    boolean isBlacklisted(String jti);
}
