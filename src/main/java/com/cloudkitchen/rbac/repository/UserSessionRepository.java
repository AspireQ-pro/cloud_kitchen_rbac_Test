package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.active = false WHERE s.userId = :userId AND s.active = true")
    void deactivateUserSessions(Integer userId);
    
    boolean existsByTokenHashAndActiveTrue(String tokenHash);
    
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.active = true AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUserId(Integer userId, LocalDateTime now);
    
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.active = true AND s.expiresAt > :now")
    int countActiveSessionsByUserId(Integer userId, LocalDateTime now);
    
    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(LocalDateTime now);
}