package com.sathya.co.biztrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sathya.co.biztrack.entity.CreditEntry;
import com.sathya.co.biztrack.entity.CreditTaker;
import com.sathya.co.biztrack.entity.Role;
import com.sathya.co.biztrack.repository.CreditDateAggregate;
import com.sathya.co.biztrack.repository.CreditEntryRepository;
import com.sathya.co.biztrack.repository.CreditMonthAggregate;
import com.sathya.co.biztrack.repository.CreditReceiptRepository;
import com.sathya.co.biztrack.repository.CreditTakerRepository;
import com.sathya.co.biztrack.service.Access;
import com.sathya.co.biztrack.service.AuditService;
import com.sathya.co.biztrack.service.CreditTakerService;
import com.sathya.co.biztrack.service.CurrentUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CreditTakerServiceTests {

    private static final Integer BUSINESS_ID = 7;
    private static final Integer TAKER_ID = 14;

    private CreditTakerRepository takers;
    private CreditEntryRepository entries;
    private CreditReceiptRepository receipts;
    private CurrentUser currentUser;
    private CreditTakerService service;

    @BeforeEach
    void setUp() {
        takers = mock(CreditTakerRepository.class);
        entries = mock(CreditEntryRepository.class);
        receipts = mock(CreditReceiptRepository.class);
        currentUser = mock(CurrentUser.class);
        service = new CreditTakerService(
                takers, entries, receipts, currentUser, mock(AuditService.class));
        when(currentUser.get()).thenReturn(new Access(3, BUSINESS_ID, Role.OWNER));
        when(currentUser.mustBe(any())).thenReturn(new Access(3, BUSINESS_ID, Role.OWNER));
        when(takers.findByIdAndBusinessId(TAKER_ID, BUSINESS_ID)).thenReturn(Optional.of(taker()));
    }

    @Test
    void perDayUsesDatabaseTotalsAndReturnsEntriesInThatDay() {
        CreditEntry entry = new CreditEntry();
        entry.setId(1);
        entry.setCreditTaker(taker());
        entry.setEntryDate(LocalDate.of(2026, 10, 1));
        entry.setEntryTime(java.time.LocalTime.of(10, 30));
        entry.setAmount(new BigDecimal("5000.00"));
        when(entries.findByBusinessIdAndCreditTaker_IdAndEntryDateBetweenOrderByEntryDateDescEntryTimeDescIdDesc(
                BUSINESS_ID, TAKER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)))
                .thenReturn(List.of(entry));
        CreditDateAggregate dailyTotal = day(1, "7500.00");
        when(entries.totalsByDate(BUSINESS_ID, TAKER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)))
                .thenReturn(List.of(dailyTotal));

        var result = service.day(TAKER_ID, LocalDate.of(2026, 10, 1));

        assertEquals(2L, result.count());
        assertEquals(new BigDecimal("7500.00"), result.dayTotal());
        assertEquals(1, result.entries().size());
    }

    @Test
    void dailyFillsEmptyDatesAndCalculatesGrandTotalFromDatabaseGroups() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 3);
        CreditDateAggregate firstDay = day(1, "7500.00");
        CreditDateAggregate thirdDay = day(3, "1200.50");
        when(entries.totalsByDate(BUSINESS_ID, TAKER_ID, from, to))
                .thenReturn(List.of(firstDay, thirdDay));

        var result = service.daily(TAKER_ID, from, to);

        assertEquals(3, result.days().size());
        assertEquals(new BigDecimal("7500.00"), result.days().get(0).total());
        assertEquals(0L, result.days().get(1).count());
        assertEquals(new BigDecimal("0.00"), result.days().get(1).total());
        assertEquals(new BigDecimal("1200.50"), result.days().get(2).total());
        assertEquals(new BigDecimal("8700.50"), result.grandTotal());
    }

    @Test
    void monthlyReturnsTwelveMonthsAndGroupsEntries() {
        CreditMonthAggregate october = month(10, 3, "8700.50");
        CreditMonthAggregate november = month(11, 1, "3000.00");
        when(entries.totalsByMonth(BUSINESS_ID, TAKER_ID, 2026))
                .thenReturn(List.of(october, november));

        var result = service.monthly(TAKER_ID, 2026);

        assertEquals(12, result.months().size());
        assertEquals(3L, result.months().get(9).count());
        assertEquals(new BigDecimal("8700.50"), result.months().get(9).total());
        assertEquals(1L, result.months().get(10).count());
        assertEquals(new BigDecimal("3000.00"), result.months().get(10).total());
        assertEquals(new BigDecimal("11700.50"), result.yearTotal());
    }

    @Test
    void emptyMonthsHaveZeroTotalsAndCounts() {
        when(entries.totalsByMonth(BUSINESS_ID, TAKER_ID, 2026)).thenReturn(List.of());

        var result = service.monthly(TAKER_ID, 2026);

        assertEquals(12, result.months().size());
        assertEquals(0L, result.months().get(0).count());
        assertEquals(new BigDecimal("0.00"), result.months().get(0).total());
        assertEquals(new BigDecimal("0.00"), result.yearTotal());
    }

    @Test
    void anotherBusinessesTakerIsNotFound() {
        when(currentUser.get()).thenReturn(new Access(9, 8, Role.OWNER));
        when(takers.summaryByBusinessIdAndId(8, TAKER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.get(TAKER_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void cannotDeleteTakerWithRetainedReceipt() {
        when(receipts.countByBusinessIdAndCreditTaker_Id(BUSINESS_ID, TAKER_ID)).thenReturn(1L);

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.delete(TAKER_ID));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(takers, never()).delete(any(CreditTaker.class));
    }

    private CreditTaker taker() {
        CreditTaker taker = new CreditTaker();
        taker.setId(TAKER_ID);
        taker.setBusinessId(BUSINESS_ID);
        taker.setName("Ravi Traders");
        return taker;
    }

    private CreditDateAggregate day(int day, String total) {
        CreditDateAggregate result = mock(CreditDateAggregate.class);
        when(result.getDate()).thenReturn(LocalDate.of(2026, 10, day));
        when(result.getEntryCount()).thenReturn(day == 1 ? 2L : 1L);
        when(result.getTotal()).thenReturn(new BigDecimal(total));
        return result;
    }

    private CreditMonthAggregate month(int month, long count, String total) {
        CreditMonthAggregate result = mock(CreditMonthAggregate.class);
        when(result.getMonth()).thenReturn(month);
        when(result.getEntryCount()).thenReturn(count);
        when(result.getTotal()).thenReturn(new BigDecimal(total));
        return result;
    }
}
