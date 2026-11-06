package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditDateSummary(LocalDate date, Long count, BigDecimal total) {
}
