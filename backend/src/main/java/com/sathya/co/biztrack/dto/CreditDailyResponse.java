package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreditDailyResponse(
        LocalDate from,
        LocalDate to,
        List<CreditDateSummary> days,
        BigDecimal grandTotal) {
}
