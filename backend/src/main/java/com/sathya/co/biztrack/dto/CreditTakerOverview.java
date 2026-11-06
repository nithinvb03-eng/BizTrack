package com.sathya.co.biztrack.dto;

import java.math.BigDecimal;

public record CreditTakerOverview(BigDecimal totalGiven, Long creditTakerCount) {
}
