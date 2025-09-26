package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.active = false WHERE s.userId = :userId AND s.active = true")
    void deactivateUserSessions(Integer userId);
    
    boolean existsByTokenHashAndActiveTrue(String tokenHash);
}