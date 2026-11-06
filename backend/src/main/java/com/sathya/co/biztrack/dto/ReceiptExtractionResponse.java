package com.sathya.co.biztrack.dto;

import java.util.List;

public record ReceiptExtractionResponse(
        boolean success,
        String message,
        String fileName,
        String extractedText,
        List<ExtractedTransaction> transactions) {
}
