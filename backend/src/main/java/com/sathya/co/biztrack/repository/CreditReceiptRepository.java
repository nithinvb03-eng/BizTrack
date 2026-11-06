package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.CreditReceipt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditReceiptRepository extends JpaRepository<CreditReceipt, Integer> {
    long countByBusinessIdAndReceiptNoStartingWith(Integer businessId, String receiptPrefix);

    long countByBusinessIdAndCreditTaker_Id(Integer businessId, Integer creditTakerId);

    Optional<CreditReceipt> findByIdAndBusinessId(Integer id, Integer businessId);

    List<CreditReceipt> findByBusinessIdAndCreditTaker_IdOrderByGeneratedAtDescIdDesc(
            Integer businessId, Integer creditTakerId);
}
