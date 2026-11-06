package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}