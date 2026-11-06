package com.sathya.co.biztrack.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreditEntryRequest(
        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("99999999.99")
        @Digits(integer = 8, fraction = 2)
        BigDecimal amount,
        @NotNull LocalDate entryDate,
        @NotNull LocalTime entryTime,
        @Size(max = 200) String note) {

    public CreditEntryRequest {
        note = note == null || note.isBlank() ? null : note.trim();
    }
}
