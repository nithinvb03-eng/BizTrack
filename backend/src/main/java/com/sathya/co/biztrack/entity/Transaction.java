package com.sathya.co.biztrack.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @Column(nullable = false)
    private Integer businessId;

    @Column(nullable = false)
    private Integer createdBy;

    @Column(nullable = false)
    private Integer categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private LocalTime transactionTime;

    private String description;

    private String paymentMethod;
}