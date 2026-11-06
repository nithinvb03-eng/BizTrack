package com.sathya.co.biztrack.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    private Integer businessId;
    private Integer userId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String recordType;

    private Long recordId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}