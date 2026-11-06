package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.CreditTakerType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record CreditReceiptResponse(
        Integer id,
        String businessName,
        String businessGstin,
        String receiptNo,
        LocalDateTime generatedAt,
        String generatedBy,
        Integer creditTakerId,
        String creditTakerName,
        String phone,
        CreditTakerType type,
        LocalDate periodFrom,
        LocalDate periodTo,
        List<ReceiptEntry> entries,
        Long entryCount,
        BigDecimal totalAmount) {

    public record ReceiptEntry(
            LocalDate date,
            LocalTime time,
            BigDecimal amount,
            String note) {
    }
}

