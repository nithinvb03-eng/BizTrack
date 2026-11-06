package com.sathya.co.biztrack.controller;

import com.sathya.co.biztrack.dto.TransactionRequest;
import com.sathya.co.biztrack.dto.ReceiptExtractionResponse;
import com.sathya.co.biztrack.dto.ReceiptDownload;
import com.sathya.co.biztrack.entity.Transaction;
import com.sathya.co.biztrack.entity.TransactionType;
import com.sathya.co.biztrack.service.CurrentUser;
import com.sathya.co.biztrack.service.ReceiptExtractionService;
import com.sathya.co.biztrack.service.TransactionService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Transactions")

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService service;

    @Autowired
    private ReceiptExtractionService receiptExtractionService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping(value = "/upload-and-extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptExtractionResponse uploadAndExtract(@RequestParam("file") MultipartFile file) {
        currentUser.get();
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose a non-empty receipt or transaction document.");
        }
        try {
            return receiptExtractionService.extract(file.getOriginalFilename(), file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The uploaded file could not be read.");
        }
    }

    @PostMapping(value = "/with-receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction addWithReceipt(
            @RequestPart("transaction") @Valid TransactionRequest request,
            @RequestPart("receipt") MultipartFile file) {
        currentUser.get();
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose a non-empty receipt or transaction document.");
        }
        try {
            byte[] content = file.getBytes();
            String contentType = receiptExtractionService.validateForStorage(
                    file.getOriginalFilename(), content);
            return service.addWithReceipt(
                    request, safeFileName(file.getOriginalFilename()), contentType, content);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The uploaded receipt could not be read.");
        }
    }

    @GetMapping
    public List<Transaction> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String q) {
        return service.list(from, to, type, categoryId, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction add(@Valid @RequestBody TransactionRequest request) {
        return service.add(request);
    }

    @PutMapping("/{id}")
    public Transaction update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> getReceipt(@PathVariable Long id) {
        ReceiptDownload receipt = service.getReceipt(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(receipt.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(receipt.fileName()).build().toString())
                .body(receipt.content());
    }

    private String safeFileName(String fileName) {
        String normalized = fileName == null ? "receipt" : fileName.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        String baseName = separator < 0 ? normalized : normalized.substring(separator + 1);
        if (baseName.isBlank()) {
            return "receipt";
        }
        return baseName.length() <= 255 ? baseName : baseName.substring(baseName.length() - 255);
    }
}