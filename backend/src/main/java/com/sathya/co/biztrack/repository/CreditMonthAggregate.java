package com.sathya.co.biztrack.repository;

import java.math.BigDecimal;

public interface CreditMonthAggregate {
    Integer getMonth();
    Long getEntryCount();
    BigDecimal getTotal();
}
