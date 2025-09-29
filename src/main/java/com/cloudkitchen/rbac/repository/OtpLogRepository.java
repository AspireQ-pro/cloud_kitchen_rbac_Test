package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.OtpLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpLogRepository extends JpaRepository<OtpLog, Integer> {
    @Query("SELECT COUNT(o) FROM OtpLog o WHERE o.phone = :phone AND o.createdOn > :after")
    int countByPhoneAndCreatedOnAfter(@Param("phone") String phone, @Param("after") LocalDateTime after);
    
    Optional<OtpLog> findTopByPhoneAndStatusOrderByCreatedOnDesc(String phone, String status);
    
    @Query("SELECT COALESCE(o.attemptsCount, 0) FROM OtpLog o WHERE o.phone = :phone AND o.status = 'sent' ORDER BY o.createdOn DESC LIMIT 1")
    Integer getCurrentAttemptCount(@Param("phone") String phone);
}
