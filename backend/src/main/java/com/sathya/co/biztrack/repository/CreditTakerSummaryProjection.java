package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.CreditTakerType;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface CreditTakerSummaryProjection {
    Integer getId();
    String getName();
    String getPhone();
    String getEmail();
    CreditTakerType getType();
    BigDecimal getTotalGiven();
    Long getEntryCount();
    LocalDate getLastEntryDate();
}
