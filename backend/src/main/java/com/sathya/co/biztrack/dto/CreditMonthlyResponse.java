package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreditMonthlyResponse(Integer year, List<CreditMonthSummary> months, BigDecimal yearTotal) {
}
