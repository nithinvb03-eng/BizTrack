package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.TransactionReceipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionReceiptRepository extends JpaRepository<TransactionReceipt, Long> {
    Optional<TransactionReceipt> findByTransactionIdAndBusinessId(Long transactionId, Integer businessId);
    void deleteByTransactionIdAndBusinessId(Long transactionId, Integer businessId);
}
