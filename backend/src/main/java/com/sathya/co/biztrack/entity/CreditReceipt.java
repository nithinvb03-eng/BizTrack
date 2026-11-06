package com.sathya.co.biztrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "credit_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_credit_receipts_business_number",
                columnNames = {"business_id", "receipt_no"}),
        indexes = @Index(name = "idx_credit_receipts_taker", columnList = "credit_taker_id"))
@Getter
@Setter
public class CreditReceipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_receipt_id")
    private Integer id;

    @Column(name = "business_id", nullable = false)
    private Integer businessId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_taker_id", nullable = false)
    private CreditTaker creditTaker;

    @Column(name = "receipt_no", nullable = false, length = 20)
    private String receiptNo;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "entry_count", nullable = false)
    private Long entryCount;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "business_gstin", length = 20)
    private String businessGstin;

    @Column(name = "generated_by_name", nullable = false, length = 200)
    private String generatedByName;

    @Column(name = "credit_taker_name", nullable = false, length = 100)
    private String creditTakerName;

    @Column(name = "credit_taker_phone", length = 13)
    private String creditTakerPhone;

    @Column(name = "credit_taker_email", length = 254)
    private String creditTakerEmail;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "credit_taker_type", nullable = false, length = 20)
    private CreditTakerType creditTakerType;
}
