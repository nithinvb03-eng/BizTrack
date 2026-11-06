package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.CreditDailyResponse;
import com.sathya.co.biztrack.dto.CreditDateSummary;
import com.sathya.co.biztrack.dto.CreditDayResponse;
import com.sathya.co.biztrack.dto.CreditEntryRequest;
import com.sathya.co.biztrack.dto.CreditEntryResponse;
import com.sathya.co.biztrack.dto.CreditMonthSummary;
import com.sathya.co.biztrack.dto.CreditMonthlyResponse;
import com.sathya.co.biztrack.dto.CreditTakerRequest;
import com.sathya.co.biztrack.dto.CreditTakerOverview;
import com.sathya.co.biztrack.dto.CreditTakerResponse;
import com.sathya.co.biztrack.entity.CreditEntry;
import com.sathya.co.biztrack.entity.CreditTaker;
import com.sathya.co.biztrack.entity.CreditTakerType;
import com.sathya.co.biztrack.entity.Role;
import com.sathya.co.biztrack.repository.CreditDateAggregate;
import com.sathya.co.biztrack.repository.CreditEntryRepository;
import com.sathya.co.biztrack.repository.CreditMonthAggregate;
import com.sathya.co.biztrack.repository.CreditReceiptRepository;
import com.sathya.co.biztrack.repository.CreditTakerRepository;
import com.sathya.co.biztrack.repository.CreditTakerOverviewProjection;
import com.sathya.co.biztrack.repository.CreditTakerSummaryProjection;
import com.sathya.co.biztrack.util.CalculationUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreditTakerService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final CreditTakerRepository takers;
    private final CreditEntryRepository entries;
    private final CreditReceiptRepository receipts;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public CreditTakerService(
            CreditTakerRepository takers,
            CreditEntryRepository entries,
            CreditReceiptRepository receipts,
            CurrentUser currentUser,
            AuditService audit) {
        this.takers = takers;
        this.entries = entries;
        this.receipts = receipts;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CreditTakerResponse> list(String query) {
        Access access = currentUser.get();
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return takers.listWithTotals(access.businessId(), normalizedQuery).stream()
                .map(CreditTakerService::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreditTakerOverview overview() {
        Access access = currentUser.get();
        CreditTakerOverviewProjection summary = takers.overview(access.businessId());
        return new CreditTakerOverview(scale(summary.getTotalGiven()), summary.getCreditTakerCount());
    }

    @Transactional(readOnly = true)
    public CreditTakerResponse get(Integer id) {
        Access access = currentUser.get();
        return takers.summaryByBusinessIdAndId(access.businessId(), id)
                .map(CreditTakerService::response)
                .orElseThrow(() -> notFound("Credit taker not found"));
    }

    @Transactional
    public CreditTakerResponse add(CreditTakerRequest request) {
        Access access = currentUser.mustBe(Role::canAdd);
        String name = normalizeName(request.name());
        String normalizedName = normalizeForSearch(name);
        if (takers.existsByBusinessIdAndNormalizedName(access.businessId(), normalizedName)) {
            throw conflict("A credit taker with this name already exists");
        }

        CreditTaker taker = new CreditTaker();
        taker.setBusinessId(access.businessId());
        taker.setCreatedBy(access.userId());
        copy(request, taker, name, normalizedName, true);
        CreditTaker saved = takers.save(taker);
        audit.log(access.businessId(), access.userId(), "CREATE", "CREDIT_TAKER", saved.getId().longValue());
        return getSummaryForWrite(saved, access.businessId());
    }

    @Transactional
    public CreditTakerResponse update(Integer id, CreditTakerRequest request) {
        Access access = currentUser.mustBe(Role::canModify);
        CreditTaker taker = findTaker(id, access.businessId());
        String name = normalizeName(request.name());
        String normalizedName = normalizeForSearch(name);
        if (takers.existsByBusinessIdAndNormalizedNameAndIdNot(
                access.businessId(), normalizedName, id)) {
            throw conflict("A credit taker with this name already exists");
        }
        copy(request, taker, name, normalizedName, false);
        takers.save(taker);
        audit.log(access.businessId(), access.userId(), "UPDATE", "CREDIT_TAKER", id.longValue());
        return getSummaryForWrite(taker, access.businessId());
    }

    @Transactional
    public void delete(Integer id) {
        Access access = currentUser.mustBe(Role::canModify);
        CreditTaker taker = findTaker(id, access.businessId());
        if (entries.countByBusinessIdAndCreditTaker_Id(access.businessId(), id) > 0) {
            throw conflict("Delete the entries first");
        }
        if (receipts.countByBusinessIdAndCreditTaker_Id(access.businessId(), id) > 0) {
            throw conflict("Credit takers with generated receipts cannot be deleted");
        }
        takers.delete(taker);
        audit.log(access.businessId(), access.userId(), "DELETE", "CREDIT_TAKER", id.longValue());
    }

    @Transactional
    public CreditEntryResponse addEntry(Integer takerId, CreditEntryRequest request) {
        Access access = currentUser.mustBe(Role::canAdd);
        CreditTaker taker = findTaker(takerId, access.businessId());
        validateEntryDate(request.entryDate());

        CreditEntry entry = new CreditEntry();
        entry.setBusinessId(access.businessId());
        entry.setCreditTaker(taker);
        entry.setCreatedBy(access.userId());
        copy(request, entry);
        CreditEntry saved = entries.save(entry);
        audit.log(access.businessId(), access.userId(), "CREATE", "CREDIT_ENTRY", saved.getId().longValue());
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<CreditEntryResponse> listEntries(Integer takerId, LocalDate from, LocalDate to) {
        Access access = currentUser.mustBe(Role::canViewReports);
        findTaker(takerId, access.businessId());
        validateRange(from, to);
        return entries
                .findByBusinessIdAndCreditTaker_IdAndEntryDateBetweenOrderByEntryDateDescEntryTimeDescIdDesc(
                        access.businessId(), takerId, from, to)
                .stream()
                .map(CreditTakerService::response)
                .toList();
    }

    @Transactional
    public CreditEntryResponse updateEntry(Integer id, CreditEntryRequest request) {
        Access access = currentUser.mustBe(Role::canModify);
        CreditEntry entry = findEntry(id, access.businessId());
        validateEntryDate(request.entryDate());
        copy(request, entry);
        entries.save(entry);
        audit.log(access.businessId(), access.userId(), "UPDATE", "CREDIT_ENTRY", id.longValue());
        return response(entry);
    }

    @Transactional
    public void deleteEntry(Integer id) {
        Access access = currentUser.mustBe(Role::canModify);
        CreditEntry entry = findEntry(id, access.businessId());
        entries.delete(entry);
        audit.log(access.businessId(), access.userId(), "DELETE", "CREDIT_ENTRY", id.longValue());
    }

    @Transactional(readOnly = true)
    public CreditDayResponse day(Integer takerId, LocalDate date) {
        Access access = currentUser.get();
        findTaker(takerId, access.businessId());
        if (date == null) {
            throw badRequest("Choose a date");
        }
        List<CreditEntryResponse> dayEntries = entries
                .findByBusinessIdAndCreditTaker_IdAndEntryDateBetweenOrderByEntryDateDescEntryTimeDescIdDesc(
                        access.businessId(), takerId, date, date)
                .stream()
                .map(CreditTakerService::response)
                .toList();
        CreditDateAggregate aggregate = entries
                .totalsByDate(access.businessId(), takerId, date, date)
                .stream()
                .findFirst()
                .orElse(null);
        BigDecimal dayTotal = aggregate == null
                ? ZERO
                : CalculationUtil.sum(List.of(scale(aggregate.getTotal())));
        return new CreditDayResponse(
                date,
                dayEntries,
                aggregate == null ? 0L : aggregate.getEntryCount(),
                scale(dayTotal));
    }

    @Transactional(readOnly = true)
    public CreditDailyResponse daily(Integer takerId, LocalDate from, LocalDate to) {
        Access access = currentUser.mustBe(Role::canViewReports);
        findTaker(takerId, access.businessId());
        validateRange(from, to);
        Map<LocalDate, CreditDateAggregate> aggregates = new HashMap<>();
        for (CreditDateAggregate aggregate : entries.totalsByDate(
                access.businessId(), takerId, from, to)) {
            aggregates.put(aggregate.getDate(), aggregate);
        }
        List<CreditDateSummary> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            CreditDateAggregate aggregate = aggregates.get(date);
            days.add(new CreditDateSummary(
                    date,
                    aggregate == null ? 0L : aggregate.getEntryCount(),
                    aggregate == null ? ZERO : scale(aggregate.getTotal())));
        }
        BigDecimal grandTotal = CalculationUtil.sum(days.stream().map(CreditDateSummary::total).toList());
        return new CreditDailyResponse(from, to, days, scale(grandTotal));
    }

    @Transactional(readOnly = true)
    public CreditMonthlyResponse monthly(Integer takerId, Integer year) {
        Access access = currentUser.mustBe(Role::canViewReports);
        findTaker(takerId, access.businessId());
        if (year == null || year < 1 || year > 9999) {
            throw badRequest("Choose a valid year");
        }
        Map<Integer, CreditMonthAggregate> aggregates = new HashMap<>();
        for (CreditMonthAggregate aggregate : entries.totalsByMonth(
                access.businessId(), takerId, year)) {
            aggregates.put(aggregate.getMonth(), aggregate);
        }
        List<CreditMonthSummary> months = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            CreditMonthAggregate aggregate = aggregates.get(month);
            months.add(new CreditMonthSummary(
                    month,
                    aggregate == null ? 0L : aggregate.getEntryCount(),
                    aggregate == null ? ZERO : scale(aggregate.getTotal())));
        }
        BigDecimal yearTotal = CalculationUtil.sum(months.stream().map(CreditMonthSummary::total).toList());
        return new CreditMonthlyResponse(year, months, scale(yearTotal));
    }

    CreditTaker findTaker(Integer id, Integer businessId) {
        return takers.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> notFound("Credit taker not found"));
    }

    private CreditEntry findEntry(Integer id, Integer businessId) {
        return entries.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> notFound("Credit entry not found"));
    }

    private CreditTakerResponse getSummaryForWrite(CreditTaker taker, Integer businessId) {
        return takers.summaryByBusinessIdAndId(businessId, taker.getId())
                .map(CreditTakerService::response)
                .orElseThrow(() -> notFound("Credit taker not found"));
    }

    private void copy(
            CreditTakerRequest request,
            CreditTaker taker,
            String name,
            String normalizedName,
            boolean creating) {
        taker.setName(name);
        taker.setNormalizedName(normalizedName);
        taker.setPhone(request.phone());
        taker.setEmail(request.email());
        if (request.type() != null) {
            taker.setType(request.type());
        } else if (creating) {
            taker.setType(CreditTakerType.CUSTOMER);
        }
    }

    private void copy(CreditEntryRequest request, CreditEntry entry) {
        entry.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        entry.setEntryDate(request.entryDate());
        entry.setEntryTime(request.entryTime());
        entry.setNote(request.note());
    }

    private void validateEntryDate(LocalDate date) {
        if (date == null) {
            throw badRequest("Choose an entry date");
        }
        if (date.isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw badRequest("Credit date cannot be in the future");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw badRequest("Choose both a start date and an end date");
        }
        if (from.isAfter(to)) {
            throw badRequest("'from' must not be after 'to'");
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.length() < 2 || normalized.length() > 100) {
            throw badRequest("Name must be between 2 and 100 characters");
        }
        return normalized;
    }

    private String normalizeForSearch(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static CreditTakerResponse response(CreditTakerSummaryProjection row) {
        return new CreditTakerResponse(
                row.getId(),
                row.getName(),
                row.getPhone(),
                row.getEmail(),
                row.getType(),
                scale(row.getTotalGiven()),
                row.getEntryCount(),
                row.getLastEntryDate());
    }

    private static CreditEntryResponse response(CreditEntry entry) {
        return new CreditEntryResponse(
                entry.getId(),
                entry.getCreditTaker().getId(),
                scale(entry.getAmount()),
                entry.getEntryDate(),
                entry.getEntryTime(),
                entry.getNote());
    }

    private static BigDecimal scale(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
