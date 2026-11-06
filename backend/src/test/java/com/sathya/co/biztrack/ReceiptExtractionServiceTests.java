package com.sathya.co.biztrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sathya.co.biztrack.dto.ReceiptExtractionResponse;
import com.sathya.co.biztrack.service.DocumentOcr;
import com.sathya.co.biztrack.service.ReceiptExtractionService;
import com.sathya.co.biztrack.service.ReceiptTransactionParser;
import com.sathya.co.biztrack.service.TabularTransactionExtractor;
import com.sathya.co.biztrack.service.TesseractDocumentOcr;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ReceiptExtractionServiceTests {

    @Test
    void extractsTextFromEveryPdfPageWithoutOcrWhenTextIsSelectable() throws Exception {
        ReceiptExtractionService service = service(image -> {
            throw new AssertionError("Selectable text should not invoke OCR");
        });

        ReceiptExtractionResponse response = service.extract("transactions.pdf", textPdf(
                "Green Basket Market\nDate: 02/10/2026 14:35\nUPI\nTransaction ID: TXN-1001\nInvoice No: INV-999\nCategory: Groceries\nDescription: Vegetables\nGST: INR 95.00\nGrand Total: INR 1,245.50",
                "Other Store\nDate: 03/10/2026\nCash\nTotal: Rs. 200.00"));

        assertEquals(2, response.transactions().size());
        assertEquals(new BigDecimal("1245.50"), response.transactions().get(0).amount());
        assertEquals(LocalDate.of(2026, 10, 2), response.transactions().get(0).date());
        assertEquals(LocalTime.of(14, 35), response.transactions().get(0).time());
        assertEquals("INR", response.transactions().get(0).currency());
        assertEquals("Green Basket Market", response.transactions().get(0).merchant());
        assertEquals("UPI", response.transactions().get(0).paymentMethod());
        assertEquals("TXN-1001", response.transactions().get(0).transactionId());
        assertEquals("INV-999", response.transactions().get(0).receiptReference());
        assertEquals("Groceries", response.transactions().get(0).category());
        assertEquals("Vegetables", response.transactions().get(0).description());
        assertEquals(new BigDecimal("95.00"), response.transactions().get(0).taxAmount());
        assertNull(response.transactions().get(0).transactionType());
        assertTrue(response.extractedText().contains("Other Store"));
    }

    @Test
    void usesOcrForImagesAndScannedPdfPages() throws Exception {
        DocumentOcr ocr = image -> "Fresh Cafe\nDate: 2/10/2026\nPaid ₹100.00";
        ReceiptExtractionService service = service(ocr);

        ReceiptExtractionResponse imageResponse = service.extract("receipt.png", png());
        ReceiptExtractionResponse scannedPdfResponse = service.extract(
                "scan.pdf", imageOnlyPdf());

        assertEquals(1, imageResponse.transactions().size());
        assertEquals(new BigDecimal("100.00"), imageResponse.transactions().get(0).amount());
        assertEquals(1, scannedPdfResponse.transactions().size());
        assertTrue(scannedPdfResponse.extractedText().contains("Fresh Cafe"));
    }

    @Test
    void recognizesTextUsingBundledTesseractAndEnglishData() throws Exception {
        ReceiptExtractionService service = service(new TesseractDocumentOcr(""));

        ReceiptExtractionResponse response = service.extract("printed-receipt.png", receiptImage());

        assertTrue(response.extractedText().toLowerCase().contains("green"));
        assertEquals(new BigDecimal("123.45"), response.transactions().get(0).amount());
    }

    @Test
    void extractsEachTransactionFromRows() {
        ReceiptTransactionParser parser = new ReceiptTransactionParser();

        var transactions = parser.parse("""
                Merchant: Green Basket
                02/10/2026 Vegetables UPI ₹1,245.50
                03/10/2026 Cleaning supplies Cash ₹200.00
                """);

        assertEquals(2, transactions.size());
        assertEquals(LocalDate.of(2026, 10, 2), transactions.get(0).date());
        assertEquals(new BigDecimal("200.00"), transactions.get(1).amount());
        assertEquals("UPI", transactions.get(0).paymentMethod());
        assertEquals("Cash", transactions.get(1).paymentMethod());
    }

    @Test
    void extractsMultipleTransactionsFromCsvRows() {
        ReceiptExtractionService service = service(image -> "");
        byte[] csv = """
                Date,Description,Amount,Category,Payment Method,Type
                2026-10-02,"Office supplies, pens",1245.50,Office,UPI,Expense
                2026-10-03,Customer payment,2000,Sales,Bank transfer,Income
                """.getBytes(StandardCharsets.UTF_8);

        ReceiptExtractionResponse response = service.extract("transactions.csv", csv);

        assertEquals(2, response.transactions().size());
        assertEquals("Office supplies, pens", response.transactions().get(0).description());
        assertEquals(new BigDecimal("1245.50"), response.transactions().get(0).amount());
        assertEquals(LocalDate.of(2026, 10, 2), response.transactions().get(0).date());
        assertEquals("UPI", response.transactions().get(0).paymentMethod());
        assertEquals("Office", response.transactions().get(0).category());
        assertEquals("INCOME", response.transactions().get(1).transactionType().name());
    }

    @Test
    void extractsTransactionsFromExcelWorkbook() throws Exception {
        ReceiptExtractionService service = service(image -> "");
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Transactions");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Transaction Date");
            header.createCell(1).setCellValue("Description");
            header.createCell(2).setCellValue("Amount");
            header.createCell(3).setCellValue("Category");
            header.createCell(4).setCellValue("Payment Method");
            var transaction = sheet.createRow(1);
            transaction.createCell(0).setCellValue("2026-10-04");
            transaction.createCell(1).setCellValue("Monthly internet");
            transaction.createCell(2).setCellValue(899.50);
            transaction.createCell(3).setCellValue("Utilities");
            transaction.createCell(4).setCellValue("Credit Card");
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        ReceiptExtractionResponse response = service.extract("transactions.xlsx", workbookBytes);

        assertEquals(1, response.transactions().size());
        assertEquals(new BigDecimal("899.50"), response.transactions().get(0).amount());
        assertEquals(LocalDate.of(2026, 10, 4), response.transactions().get(0).date());
        assertEquals("Monthly internet", response.transactions().get(0).description());
        assertEquals("Utilities", response.transactions().get(0).category());
        assertEquals("Credit Card", response.transactions().get(0).paymentMethod());
        assertEquals(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                service.validateForStorage("transactions.xlsx", workbookBytes));
    }

    @Test
    void extractsMultipleTransactionsWhenFieldsSpanSeparateLines() {
        ReceiptTransactionParser parser = new ReceiptTransactionParser();

        var transactions = parser.parse("""
                Merchant: Green Basket
                Date: 02/10/2026
                Description: Groceries
                Amount: INR 1,000.00
                Date: 03/10/2026
                Description: Cleaning supplies
                Amount: INR 200
                """);

        assertEquals(2, transactions.size());
        assertEquals(new BigDecimal("1000.00"), transactions.get(0).amount());
        assertEquals(new BigDecimal("200.00"), transactions.get(1).amount());
        assertEquals("Cleaning supplies", transactions.get(1).description());
    }

    @Test
    void rejectsUnsupportedEmptyCorruptAndUnreadableFiles() throws Exception {
        ReceiptExtractionService service = service(image -> "");

        assertEquals("application/pdf",
                service.validateForStorage("receipt.pdf", textPdf("Merchant\nTotal: INR 20.00")));
        assertEquals("image/png", service.validateForStorage("receipt.png", png()));
        assertEquals("text/csv", service.validateForStorage(
                "transactions.csv",
                "Date,Amount\n2026-10-01,25.00".getBytes(StandardCharsets.UTF_8)));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.extract("empty.pdf", new byte[0]));
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, () -> service.extract("receipt.webp", new byte[] {1}));
        assertStatus(HttpStatus.UNPROCESSABLE_ENTITY, () -> service.extract("transactions.xlsx", new byte[] {1}));
        assertStatus(HttpStatus.UNPROCESSABLE_ENTITY, () -> service.extract("broken.pdf", "%PDF-broken".getBytes()));
        assertStatus(HttpStatus.UNPROCESSABLE_ENTITY, () -> service.extract("empty-scan.png", png()));
        assertStatus(HttpStatus.PAYLOAD_TOO_LARGE,
                () -> service.extract("oversized-page.pdf", oversizedPdf()));
        assertStatus(HttpStatus.PAYLOAD_TOO_LARGE,
                () -> new ReceiptExtractionService(
                        image -> "",
                        new ReceiptTransactionParser(),
                        new TabularTransactionExtractor(),
                        1)
                        .extract("large.png", png()));
    }

    @Test
    void reportsOcrFailuresInsteadOfReturningEmptyResults() throws Exception {
        ReceiptExtractionService service = service(image -> {
            throw new TesseractException("OCR failure");
        });

        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> service.extract("scan.png", png()));
    }

    private ReceiptExtractionService service(DocumentOcr ocr) {
        return new ReceiptExtractionService(
                ocr,
                new ReceiptTransactionParser(),
                new TabularTransactionExtractor(),
                10 * 1024 * 1024);
    }

    private byte[] textPdf(String... pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String text : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.setLeading(16);
                    stream.newLineAtOffset(50, 750);
                    for (String line : text.split("\\n")) {
                        stream.showText(line);
                        stream.newLine();
                    }
                    stream.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] imageOnlyPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDImageXObject image = LosslessFactory.createFromImage(document,
                    new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB));
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.drawImage(image, 50, 50, 100, 100);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] oversizedPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(new PDRectangle(15_000, 15_000)));
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private byte[] receiptImage() throws IOException {
        BufferedImage image = new BufferedImage(1200, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 58));
        graphics.drawString("GREEN BASKET MARKET", 50, 150);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 48));
        graphics.drawString("TOTAL INR 123.45", 50, 260);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private void assertStatus(HttpStatus status, Executable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action::execute);
        assertEquals(status, exception.getStatusCode());
    }
}
