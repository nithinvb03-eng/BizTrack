package com.sathya.co.biztrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "credit_entries",
        indexes = @Index(
                name = "idx_credit_entries_taker_date",
                columnList = "credit_taker_id, entry_date"))
@Getter
@Setter
public class CreditEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_entry_id")
    private Integer id;

    @Column(name = "business_id", nullable = false)
    private Integer businessId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_taker_id", nullable = false)
    private CreditTaker creditTaker;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @Column(length = 200)
    private String note;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;
}
