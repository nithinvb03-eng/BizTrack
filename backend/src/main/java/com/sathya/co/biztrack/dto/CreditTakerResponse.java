package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.CreditTakerType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditTakerResponse(
        Integer id,
        String name,
        String phone,
        String email,
        CreditTakerType type,
        BigDecimal totalGiven,
        Long entryCount,
        LocalDate lastEntryDate) {
}
