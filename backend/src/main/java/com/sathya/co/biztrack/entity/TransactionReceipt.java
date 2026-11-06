package com.sathya.co.biztrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction_receipts",
        uniqueConstraints = @UniqueConstraint(columnNames = "transaction_id"))
@Getter
@Setter
public class TransactionReceipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id", nullable = false)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "business_id", nullable = false)
    private Integer businessId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;
}
