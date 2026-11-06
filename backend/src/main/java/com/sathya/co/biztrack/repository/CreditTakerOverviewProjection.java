package com.sathya.co.biztrack.repository;

import java.math.BigDecimal;

public interface CreditTakerOverviewProjection {
    BigDecimal getTotalGiven();
    Long getCreditTakerCount();
}
