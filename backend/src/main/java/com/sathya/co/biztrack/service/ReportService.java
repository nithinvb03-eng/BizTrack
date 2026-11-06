package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.*;
import com.sathya.co.biztrack.entity.Role;
import com.sathya.co.biztrack.entity.TransactionType;
import com.sathya.co.biztrack.repository.TransactionRepository;
import com.sathya.co.biztrack.util.CalculationUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportService {

    @Autowired private TransactionRepository transactions;
    @Autowired private CurrentUser currentUser;

    /** Totals above the ledger: inflow, outflow, profit, margin for a date range. */
    public SummaryResponse summary(LocalDate from, LocalDate to) {
        Access a = currentUser.get();
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.withDayOfMonth(1);

        BigDecimal inflow = total(a.businessId(), TransactionType.INCOME, start, end);
        BigDecimal outflow = total(a.businessId(), TransactionType.EXPENSE, start, end);
        BigDecimal profit = CalculationUtil.profit(inflow, outflow);
        return new SummaryResponse(inflow, outflow, profit, CalculationUtil.percent(profit, inflow));
    }

    /** Dashboard: the last 6 months including this one. */
    public DashboardResponse dashboard() {
        Access a = currentUser.get();
        YearMonth now = YearMonth.now();

        List<MonthRow> months = new ArrayList<>();
        List<BigDecimal> inflows = new ArrayList<>();
        List<BigDecimal> outflows = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            MonthRow row = monthRow(a.businessId(), now.minusMonths(i));
            months.add(row);
            inflows.add(row.inflow());
            outflows.add(row.outflow());
        }

        BigDecimal totalInflow = CalculationUtil.sum(inflows);
        BigDecimal totalOutflow = CalculationUtil.sum(outflows);
        BigDecimal netProfit = CalculationUtil.profit(totalInflow, totalOutflow);

        MonthRow first = months.get(0);
        MonthRow previous = months.get(4);
        MonthRow current = months.get(5);

        MonthRow best = first;                       // month with the highest profit (surplus)
        for (MonthRow m : months) {
            if (m.profit().compareTo(best.profit()) > 0) {
                best = m;
            }
        }

        return new DashboardResponse(
                totalInflow, totalOutflow, netProfit, CalculationUtil.percent(netProfit, totalInflow),
                CalculationUtil.percentChange(current.inflow(), previous.inflow()),
                CalculationUtil.percentChange(current.outflow(), previous.outflow()),
                CalculationUtil.ratio(totalInflow, totalOutflow),
                CalculationUtil.average(totalInflow, months.size()),
                CalculationUtil.average(totalOutflow, months.size()),
                CalculationUtil.percentChange(current.inflow(), first.inflow()),
                best.month(), best.profit(), months);
    }

    /** Monthly report: one row per month between from and to (default: last 3 months). */
    public List<MonthRow> monthlyReport(LocalDate from, LocalDate to) {
        Access a = currentUser.mustBe(Role::canViewReports);
        YearMonth end = (to != null) ? YearMonth.from(to) : YearMonth.now();
        YearMonth start = (from != null) ? YearMonth.from(from) : end.minusMonths(2);
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
        }
        List<MonthRow> rows = new ArrayList<>();
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            rows.add(monthRow(a.businessId(), ym));
        }
        return rows;
    }

    private MonthRow monthRow(Integer businessId, YearMonth ym) {
        BigDecimal inflow = total(businessId, TransactionType.INCOME, ym.atDay(1), ym.atEndOfMonth());
        BigDecimal outflow = total(businessId, TransactionType.EXPENSE, ym.atDay(1), ym.atEndOfMonth());
        BigDecimal profit = CalculationUtil.profit(inflow, outflow);
        String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear();
        return new MonthRow(label, inflow, outflow, profit, CalculationUtil.percent(profit, inflow));
    }

    // The database SUM returns null when there are no rows, so turn that into 0
    private BigDecimal total(Integer businessId, TransactionType type, LocalDate from, LocalDate to) {
        BigDecimal sum = transactions.sumByType(businessId, type, from, to);
        return (sum == null) ? BigDecimal.ZERO : sum;
    }
}