package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;

public record CreditMonthSummary(Integer month, Long count, BigDecimal total) {
}
