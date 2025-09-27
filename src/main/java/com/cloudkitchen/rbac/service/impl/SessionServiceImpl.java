package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.UserSession;
import com.cloudkitchen.rbac.repository.UserSessionRepository;
import com.cloudkitchen.rbac.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionServiceImpl implements SessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);
    private final UserSessionRepository sessionRepo;
    
    public SessionServiceImpl(UserSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }
    
    @Override
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredSessions() {
        cleanupExpiredSessionsBatch();
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void cleanupExpiredSessionsBatch() {
        try {
            int deletedCount = sessionRepo.deleteExpiredSessions(LocalDateTime.now());
            if (deletedCount > 0) {
                logger.info("Cleaned up {} expired sessions", deletedCount);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup expired sessions", e);
        }
    }
    
    @Override
    public List<UserSession> getActiveSessions(Integer userId) {
        return sessionRepo.findActiveSessionsByUserId(userId, LocalDateTime.now());
    }
    
    @Override
    @Transactional
    public void deactivateAllUserSessions(Integer userId) {
        sessionRepo.deactivateUserSessions(userId);
        logger.info("Deactivated all sessions for user: {}", userId);
    }
    
    @Override
    public boolean isSessionValid(String tokenHash) {
        return sessionRepo.existsByTokenHashAndExpiresAtAfter(tokenHash, LocalDateTime.now());
    }
    
    @Override
    public int getActiveSessionCount(Integer userId) {
        return sessionRepo.countActiveSessionsByUserId(userId, LocalDateTime.now());
    }
}