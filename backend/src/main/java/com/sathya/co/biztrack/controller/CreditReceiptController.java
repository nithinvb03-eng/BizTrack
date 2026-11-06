package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.dto.CreditEmailRequest;
import com.sathya.co.biztrack.service.CreditReceiptService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Credits")
@RestController
@RequestMapping("/api/credit-receipts")
public class CreditReceiptController {

    private final CreditReceiptService receiptService;

    public CreditReceiptController(CreditReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/{receiptId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Integer receiptId) {
        byte[] content = receiptService.pdf(receiptId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("credit-receipt-" + receiptId + ".pdf")
                                .build()
                                .toString())
                .body(content);
    }

    @PostMapping("/{receiptId}/email")
    public ResponseEntity<Void> email(
            @PathVariable Integer receiptId, @Valid @RequestBody CreditEmailRequest request) {
        receiptService.email(receiptId, request);
        return ResponseEntity.noContent().build();
    }
}
