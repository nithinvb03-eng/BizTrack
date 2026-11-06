package com.sathya.co.biztrack.dto;

import com.sathya.co.biztrack.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TransactionRequest(
        @NotNull TransactionType transactionType,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate transactionDate,
        LocalTime transactionTime,
        @NotNull Integer categoryId,
        @NotBlank String description,
        String paymentMethod) {
}