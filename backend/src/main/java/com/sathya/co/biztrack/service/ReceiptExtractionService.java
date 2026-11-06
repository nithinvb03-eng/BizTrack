package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.ExtractedTransaction;
import com.sathya.co.biztrack.dto.ReceiptExtractionResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReceiptExtractionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptExtractionService.class);
    private static final int MAX_IMAGE_PIXELS = 40_000_000;

    private final DocumentOcr ocr;
    private final ReceiptTransactionParser parser;
    private final TabularTransactionExtractor tabularExtractor;
    private final long maxFileSize;

    public ReceiptExtractionService(
            DocumentOcr ocr,
            ReceiptTransactionParser parser,
            TabularTransactionExtractor tabularExtractor,
            @Value("${app.receipts.max-size:10485760}") long maxFileSize) {
        this.ocr = ocr;
        this.parser = parser;
        this.tabularExtractor = tabularExtractor;
        this.maxFileSize = maxFileSize;
    }

    public ReceiptExtractionResponse extract(String fileName, byte[] bytes) {
        String extension = validateUpload(fileName, bytes);
        String text = switch (extension) {
            case "pdf" -> extractPdf(bytes);
            case "png", "jpg", "jpeg" -> extractImage(bytes);
            case "csv" -> tabularExtractor.extractCsv(bytes);
            case "xls", "xlsx" -> extractExcel(bytes);
            default -> throw new IllegalStateException("Validated file extension was not handled.");
        };
        if (text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "No readable text could be extracted from this file.");
        }
        List<ExtractedTransaction> transactions = parser.parse(text);
        if (transactions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Readable text was found, but no transaction details could be identified.");
        }
        LOGGER.info("Processed transaction document: type={}, characters={}, transactions={}",
                extension, text.length(), transactions.size());
        return new ReceiptExtractionResponse(true, "File processed successfully", fileName, text, transactions);
    }

    public String validateForStorage(String fileName, byte[] bytes) {
        String extension = validateUpload(fileName, bytes);
        if (extension.equals("pdf")) {
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.getNumberOfPages() == 0) {
                    throw new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY, "The PDF contains no pages.");
                }
            } catch (IOException exception) {
                LOGGER.warn("Receipt PDF validation failed: {}", exception.getClass().getSimpleName());
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "The PDF file is corrupted or could not be read.");
            }
        } else if (extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg")) {
            readImage(bytes);
        } else if (extension.equals("xls") || extension.equals("xlsx")) {
            extractExcel(bytes);
        } else if (extension.equals("csv")) {
            tabularExtractor.extractCsv(bytes);
        }
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "csv" -> "text/csv";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> throw new IllegalStateException("Validated file extension was not handled.");
        };
    }

    private String validateUpload(String fileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is empty.");
        }
        if (bytes.length > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Files must be 10 MB or smaller.");
        }

        String extension = extension(fileName);
        boolean validSignature = switch (extension) {
            case "pdf" -> startsWith(bytes, "%PDF-".getBytes());
            case "png" -> bytes.length >= 8 && startsWith(
                    bytes, new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            case "jpg", "jpeg" -> bytes.length >= 3 &&
                    bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff;
            case "xls" -> bytes.length >= 8 && startsWith(
                    bytes, new byte[] {(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0,
                            (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1});
            case "xlsx" -> bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' &&
                    bytes[2] == 3 && bytes[3] == 4;
            case "csv" -> isTextFile(bytes);
            default -> throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Supported files are PDF, JPG, JPEG, PNG, CSV, XLS, and XLSX.");
        };
        if (!validSignature) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "The uploaded file is corrupted or its extension does not match its contents.");
        }
        return extension;
    }

    private String extractExcel(byte[] bytes) {
        try {
            return tabularExtractor.extractExcel(bytes);
        } catch (IOException exception) {
            LOGGER.warn("Spreadsheet parsing failed: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The Excel file is corrupted or could not be read.");
        }
    }

    private boolean isTextFile(byte[] bytes) {
        for (byte value : bytes) {
            if (value == 0) {
                return false;
            }
        }
        return true;
    }

    private String extractPdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                if (pageText.isBlank()) {
                    LOGGER.debug("Running OCR on PDF page {}", page);
                    ensurePdfPageSize(document.getPage(page - 1));
                    BufferedImage image = renderer.renderImageWithDPI(page - 1, 200f, ImageType.GRAY);
                    ensureImageSize(image);
                    pageText = recognize(image);
                }
                if (!pageText.isBlank()) {
                    if (!text.isEmpty()) {
                        text.append('\f');
                    }
                    text.append(pageText);
                }
            }
            return text.toString().trim();
        } catch (IOException exception) {
            LOGGER.warn("Receipt PDF parsing failed: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "The PDF file is corrupted or could not be read.");
        }
    }

    private String extractImage(byte[] bytes) {
        BufferedImage image = readImage(bytes);
        LOGGER.debug("Running OCR on uploaded image: width={}, height={}",
                image.getWidth(), image.getHeight());
        return recognize(image);
    }

    private BufferedImage readImage(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "The image file is corrupted or could not be read.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "The image file is corrupted or could not be read.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                ensureImageSize(reader.getWidth(0), reader.getHeight(0));
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            LOGGER.warn("Receipt image validation failed: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "The image file is corrupted or could not be read.");
        }
    }

    private String recognize(BufferedImage image) {
        try {
            return ocr.recognize(image).trim();
        } catch (TesseractException | LinkageError exception) {
            LOGGER.error("Receipt OCR failed: {}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "OCR is unavailable. Configure Tesseract and its English language data.");
        }
    }

    private void ensureImageSize(BufferedImage image) {
        ensureImageSize(image.getWidth(), image.getHeight());
    }

    private void ensureImageSize(int width, int height) {
        if ((long) width * height > MAX_IMAGE_PIXELS) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "The document page dimensions exceed the supported limit.");
        }
    }

    private void ensurePdfPageSize(PDPage page) {
        double width = page.getCropBox().getWidth() * 200.0 / 72.0;
        double height = page.getCropBox().getHeight() * 200.0 / 72.0;
        if (width <= 0 || height <= 0 || width * height > MAX_IMAGE_PIXELS) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "The document page dimensions exceed the supported limit.");
        }
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }
}
