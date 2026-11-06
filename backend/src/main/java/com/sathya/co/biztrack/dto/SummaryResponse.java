package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;

public record SummaryResponse(BigDecimal inflow, BigDecimal outflow,
                              BigDecimal profit, BigDecimal marginPercent) {
}