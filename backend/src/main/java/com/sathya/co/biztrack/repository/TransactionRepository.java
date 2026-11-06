package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.Transaction;
import com.sathya.co.biztrack.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByBusinessIdAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(
            Integer businessId, LocalDate from, LocalDate to);

    Optional<Transaction> findByIdAndBusinessId(Long id, Integer businessId);

    // SUM of amounts of one type (INCOME or EXPENSE) in a date range. Returns null if there are no rows.
    @Query("select sum(t.amount) from Transaction t "
            + "where t.businessId = :businessId and t.transactionType = :type "
            + "and t.transactionDate between :from and :to")
    BigDecimal sumByType(@Param("businessId") Integer businessId,
                         @Param("type") TransactionType type,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);
}