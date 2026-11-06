package com.sathya.co.biztrack.repository;

import com.sathya.co.biztrack.entity.CreditTaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditTakerRepository extends JpaRepository<CreditTaker, Integer> {

    @Query(value = """
            select ct.credit_taker_id as id, ct.name as name, ct.phone as phone,
                   ct.email as email, ct.type as type,
                   coalesce(sum(e.amount), 0.00) as totalGiven,
                   count(e.credit_entry_id) as entryCount,
                   max(e.entry_date) as lastEntryDate
            from credit_takers ct
            left join credit_entries e
              on e.credit_taker_id = ct.credit_taker_id and e.business_id = ct.business_id
            where ct.business_id = :businessId
              and (:query is null or lower(ct.name) like concat('%', lower(:query), '%'))
            group by ct.credit_taker_id, ct.name, ct.phone, ct.email, ct.type
            order by ct.name
            """, nativeQuery = true)
    List<CreditTakerSummaryProjection> listWithTotals(
            @Param("businessId") Integer businessId,
            @Param("query") String query);

    @Query(value = """
            select coalesce(sum(e.amount), 0.00) as totalGiven,
                   count(distinct ct.credit_taker_id) as creditTakerCount
            from credit_takers ct
            left join credit_entries e
              on e.credit_taker_id = ct.credit_taker_id and e.business_id = ct.business_id
            where ct.business_id = :businessId
            """, nativeQuery = true)
    CreditTakerOverviewProjection overview(@Param("businessId") Integer businessId);

    @Query(value = """
            select ct.credit_taker_id as id, ct.name as name, ct.phone as phone,
                   ct.email as email, ct.type as type,
                   coalesce(sum(e.amount), 0.00) as totalGiven,
                   count(e.credit_entry_id) as entryCount,
                   max(e.entry_date) as lastEntryDate
            from credit_takers ct
            left join credit_entries e
              on e.credit_taker_id = ct.credit_taker_id and e.business_id = ct.business_id
            where ct.business_id = :businessId and ct.credit_taker_id = :id
            group by ct.credit_taker_id, ct.name, ct.phone, ct.email, ct.type
            """, nativeQuery = true)
    Optional<CreditTakerSummaryProjection> summaryByBusinessIdAndId(
            @Param("businessId") Integer businessId,
            @Param("id") Integer id);

    Optional<CreditTaker> findByIdAndBusinessId(Integer id, Integer businessId);

    boolean existsByBusinessIdAndNormalizedName(Integer businessId, String normalizedName);

    boolean existsByBusinessIdAndNormalizedNameAndIdNot(
            Integer businessId, String normalizedName, Integer id);
}
