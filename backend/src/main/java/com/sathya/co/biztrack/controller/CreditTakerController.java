package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.dto.CreditDailyResponse;
import com.sathya.co.biztrack.dto.CreditDayResponse;
import com.sathya.co.biztrack.dto.CreditEntryRequest;
import com.sathya.co.biztrack.dto.CreditEntryResponse;
import com.sathya.co.biztrack.dto.CreditMonthlyResponse;
import com.sathya.co.biztrack.dto.CreditReceiptListItem;
import com.sathya.co.biztrack.dto.CreditReceiptResponse;
import com.sathya.co.biztrack.dto.CreditTakerRequest;
import com.sathya.co.biztrack.dto.CreditTakerOverview;
import com.sathya.co.biztrack.dto.CreditTakerResponse;
import com.sathya.co.biztrack.service.CreditReceiptService;
import com.sathya.co.biztrack.service.CreditTakerService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Credits")
@RestController
@RequestMapping("/api/credit-takers")
public class CreditTakerController {

    private final CreditTakerService takerService;
    private final CreditReceiptService receiptService;

    public CreditTakerController(CreditTakerService takerService, CreditReceiptService receiptService) {
        this.takerService = takerService;
        this.receiptService = receiptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditTakerResponse add(@Valid @RequestBody CreditTakerRequest request) {
        return takerService.add(request);
    }

    @GetMapping
    public List<CreditTakerResponse> list(@RequestParam(required = false) String q) {
        return takerService.list(q);
    }

    @GetMapping("/overview")
    public CreditTakerOverview overview() {
        return takerService.overview();
    }

    @GetMapping("/{id}")
    public CreditTakerResponse get(@PathVariable Integer id) {
        return takerService.get(id);
    }

    @PutMapping("/{id}")
    public CreditTakerResponse update(
            @PathVariable Integer id, @Valid @RequestBody CreditTakerRequest request) {
        return takerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        takerService.delete(id);
    }

    @PostMapping("/{id}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditEntryResponse addEntry(
            @PathVariable Integer id, @Valid @RequestBody CreditEntryRequest request) {
        return takerService.addEntry(id, request);
    }

    @GetMapping("/{id}/entries")
    public List<CreditEntryResponse> entries(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return takerService.listEntries(id, from, to);
    }

    @GetMapping("/{id}/day")
    public CreditDayResponse day(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return takerService.day(id, date);
    }

    @GetMapping("/{id}/daily")
    public CreditDailyResponse daily(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return takerService.daily(id, from, to);
    }

    @GetMapping("/{id}/monthly")
    public CreditMonthlyResponse monthly(@PathVariable Integer id, @RequestParam Integer year) {
        return takerService.monthly(id, year);
    }

    @GetMapping("/{id}/receipt-preview")
    public CreditReceiptResponse preview(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return receiptService.preview(id, from, to);
    }

    @GetMapping("/{id}/receipt")
    public CreditReceiptResponse generate(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return receiptService.generate(id, from, to);
    }

    @GetMapping("/{id}/receipts")
    public List<CreditReceiptListItem> receipts(@PathVariable Integer id) {
        return receiptService.list(id);
    }
}
