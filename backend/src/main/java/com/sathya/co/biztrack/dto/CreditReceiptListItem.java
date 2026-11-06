package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreditReceiptListItem(
        Integer id,
        String receiptNo,
        LocalDate periodFrom,
        LocalDate periodTo,
        BigDecimal totalAmount,
        Long entryCount,
        LocalDateTime generatedAt) {
}
