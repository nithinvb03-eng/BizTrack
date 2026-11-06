package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.dto.CreditEntryRequest;
import com.sathya.co.biztrack.dto.CreditEntryResponse;
import com.sathya.co.biztrack.service.CreditTakerService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Credits")
@RestController
@RequestMapping("/api/credit-entries")
public class CreditEntryController {

    private final CreditTakerService takerService;

    public CreditEntryController(CreditTakerService takerService) {
        this.takerService = takerService;
    }

    @PutMapping("/{entryId}")
    public CreditEntryResponse update(
            @PathVariable Integer entryId, @Valid @RequestBody CreditEntryRequest request) {
        return takerService.updateEntry(entryId, request);
    }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer entryId) {
        takerService.deleteEntry(entryId);
    }
}
