package com.sathya.co.biztrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "credit_takers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_credit_takers_business_normalized_name",
                columnNames = {"business_id", "normalized_name"}),
        indexes = @Index(name = "idx_credit_takers_business", columnList = "business_id"))
@Getter
@Setter
public class CreditTaker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_taker_id")
    private Integer id;

    @Column(name = "business_id", nullable = false)
    private Integer businessId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    @Column(length = 13)
    private String phone;

    @Column(length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CreditTakerType type = CreditTakerType.CUSTOMER;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;
}
