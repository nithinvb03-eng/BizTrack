package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreditEntryResponse(
        Integer id,
        Integer creditTakerId,
        BigDecimal amount,
        LocalDate entryDate,
        LocalTime entryTime,
        String note) {
}
