package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.OtpLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpLogRepository extends JpaRepository<OtpLog, Integer> {
}
