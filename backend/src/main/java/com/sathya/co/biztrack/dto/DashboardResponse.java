package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal totalInflow, BigDecimal totalOutflow, BigDecimal netProfit, BigDecimal marginPercent,
        BigDecimal inflowVsLastMonthPercent, BigDecimal outflowVsLastMonthPercent,
        BigDecimal cashRatio, BigDecimal avgMonthlyInflow, BigDecimal avgMonthlyOutflow,
        BigDecimal inflowGrowthPercent, String highestSurplusMonth, BigDecimal highestSurplusAmount,
        List<MonthRow> months) {
}