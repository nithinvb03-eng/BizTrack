package com.sathya.co.biztrack.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CreditDateAggregate {
    LocalDate getDate();
    Long getEntryCount();
    BigDecimal getTotal();
}
