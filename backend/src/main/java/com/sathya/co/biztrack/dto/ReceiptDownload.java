package com.sathya.co.biztrack.dto;

public record ReceiptDownload(String fileName, String contentType, byte[] content) {
}
