package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ExtractedTransaction(
        String merchant,
        BigDecimal amount,
        String currency,
        LocalDate date,
        LocalTime time,
        String description,
        String category,
        String paymentMethod,
        String transactionId,
        BigDecimal taxAmount,
        String receiptReference,
        TransactionType transactionType,
        BigDecimal confidence) {
}
