package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.CreditReceiptLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditReceiptLineRepository extends JpaRepository<CreditReceiptLine, Integer> {
    List<CreditReceiptLine> findByReceipt_IdOrderByEntryDateAscEntryTimeAscIdAsc(Integer receiptId);
}
