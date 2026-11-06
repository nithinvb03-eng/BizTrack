package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.entity.AuditLog;
import com.sathya.co.biztrack.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository repo;

    public void log(Integer businessId, Integer userId, String action, String recordType, Long recordId) {
        AuditLog entry = new AuditLog();
        entry.setBusinessId(businessId);
        entry.setUserId(userId);
        entry.setAction(action);
        entry.setRecordType(recordType);
        entry.setRecordId(recordId);
        repo.save(entry);
    }
}