package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.CreditEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditEntryRepository extends JpaRepository<CreditEntry, Integer> {

    List<CreditEntry> findByBusinessIdAndCreditTaker_IdAndEntryDateBetweenOrderByEntryDateDescEntryTimeDescIdDesc(
            Integer businessId, Integer creditTakerId, LocalDate from, LocalDate to);

    List<CreditEntry> findByBusinessIdAndCreditTaker_IdAndEntryDateBetweenOrderByEntryDateAscEntryTimeAscIdAsc(
            Integer businessId, Integer creditTakerId, LocalDate from, LocalDate to);

    Optional<CreditEntry> findByIdAndBusinessId(Integer id, Integer businessId);

    long countByBusinessIdAndCreditTaker_Id(Integer businessId, Integer creditTakerId);

    @Query("""
            select e.entryDate as date, count(e.id) as entryCount, sum(e.amount) as total
            from CreditEntry e
            where e.businessId = :businessId and e.creditTaker.id = :creditTakerId
              and e.entryDate between :from and :to
            group by e.entryDate
            order by e.entryDate
            """)
    List<CreditDateAggregate> totalsByDate(
            @Param("businessId") Integer businessId,
            @Param("creditTakerId") Integer creditTakerId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(value = """
            select month(entry_date) as month, count(credit_entry_id) as entryCount,
                   sum(amount) as total
            from credit_entries
            where business_id = :businessId and credit_taker_id = :creditTakerId
              and year(entry_date) = :year
            group by month(entry_date)
            order by month(entry_date)
            """, nativeQuery = true)
    List<CreditMonthAggregate> totalsByMonth(
            @Param("businessId") Integer businessId,
            @Param("creditTakerId") Integer creditTakerId,
            @Param("year") Integer year);
}
