package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.dto.DashboardResponse;
import com.sathya.co.biztrack.dto.MonthRow;
import com.sathya.co.biztrack.dto.SummaryResponse;
import com.sathya.co.biztrack.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reports")

@RestController
@RequestMapping("/api")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.summary(from, to);
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return reportService.dashboard();
    }

    @GetMapping("/reports/monthly")
    public List<MonthRow> monthly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.monthlyReport(from, to);
    }
}