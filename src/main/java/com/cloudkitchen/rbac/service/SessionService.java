package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.entity.UserSession;
import java.util.List;

public interface SessionService {
    void cleanupExpiredSessions();
    List<UserSession> getActiveSessions(Integer userId);
    void deactivateAllUserSessions(Integer userId);
    boolean isSessionValid(String tokenHash);
    int getActiveSessionCount(Integer userId);
}