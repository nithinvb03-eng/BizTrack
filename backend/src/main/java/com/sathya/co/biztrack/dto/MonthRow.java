package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;

public record MonthRow(String month, BigDecimal inflow, BigDecimal outflow,
                       BigDecimal profit, BigDecimal marginPercent) {
}