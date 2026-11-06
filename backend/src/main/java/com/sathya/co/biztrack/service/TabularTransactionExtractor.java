package com.sathya.co.biztrack.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TabularTransactionExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TabularTransactionExtractor.class);
    private static final int MAX_ROWS = 10_000;
    private static final Set<String> DATE_HEADERS = Set.of(
            "date", "transactiondate", "transaction_date", "transdate", "trans_date");
    private static final Set<String> AMOUNT_HEADERS = Set.of(
            "amount", "amountinr", "transactionamount", "transaction_amount",
            "total", "totalamount", "total_amount", "paid", "debit", "credit", "value");
    private static final Map<String, Set<String>> FIELD_HEADERS = Map.of(
            "date", DATE_HEADERS,
            "amount", AMOUNT_HEADERS,
            "description", Set.of("description", "details", "narration", "particulars", "memo", "item"),
            "merchant", Set.of("merchant", "vendor", "store", "payee", "business"),
            "category", Set.of("category", "expensecategory", "expense_category",
                    "transactioncategory", "transaction_category"),
            "payment", Set.of("paymentmethod", "payment_method", "payment", "mode", "method"),
            "type", Set.of("transactiontype", "transaction_type", "type", "direction"),
            "reference", Set.of("transactionid", "transaction_id", "reference", "ref", "receipt"));

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("M/d/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("M-d-uuuu"),
            DateTimeFormatter.ofPattern("d.M.uuuu"),
            DateTimeFormatter.ofPattern("d/M/uu"),
            DateTimeFormatter.ofPattern("M/d/uu"),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH));

    public String extractCsv(byte[] bytes) {
        String csv = new String(bytes, StandardCharsets.UTF_8);
        if (!csv.isEmpty() && csv.charAt(0) == '\ufeff') {
            csv = csv.substring(1);
        }
        List<List<String>> rows = parseCsv(csv);
        return toTransactionText(rows);
    }

    public String extractExcel(byte[] bytes) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
            List<List<String>> rows = new ArrayList<>();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                List<List<String>> sheetRows = new ArrayList<>();
                for (Row row : sheet) {
                    if (sheetRows.size() >= MAX_ROWS + 20) {
                        throw new ResponseStatusException(
                                HttpStatus.PAYLOAD_TOO_LARGE,
                                "The spreadsheet exceeds the 10,000 row limit.");
                    }
                    List<String> values = new ArrayList<>();
                    int columnCount = Math.min(row.getLastCellNum(), 100);
                    for (int column = 0; column < columnCount; column++) {
                        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        values.add(formatCell(cell, formatter));
                    }
                    sheetRows.add(values);
                }
                if (rows.isEmpty()) {
                    rows = sheetRows;
                }
                if (containsTransactionHeaders(sheetRows)) {
                    rows = sheetRows;
                    break;
                }
            }
            return toTransactionText(rows);
        }
    }

    private String formatCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return formatter.formatCellValue(cell).trim();
    }

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        char delimiter = detectDelimiter(text);
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < text.length() && text.charAt(index + 1) == '"') {
                    cell.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (!quoted && current == delimiter) {
                row.add(cell.toString().trim());
                cell.setLength(0);
            } else if (!quoted && (current == '\n' || current == '\r')) {
                if (current == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(cell.toString().trim());
                cell.setLength(0);
                if (row.stream().anyMatch(value -> !value.isBlank())) {
                    rows.add(row);
                    if (rows.size() > MAX_ROWS + 20) {
                        throw new ResponseStatusException(
                                HttpStatus.PAYLOAD_TOO_LARGE,
                                "The spreadsheet exceeds the 10,000 row limit.");
                    }
                }
                row = new ArrayList<>();
            } else {
                cell.append(current);
            }
        }
        if (quoted) {
            throw invalidTabularFile();
        }
        row.add(cell.toString().trim());
        if (row.stream().anyMatch(value -> !value.isBlank())) {
            rows.add(row);
        }
        return rows;
    }

    private boolean containsTransactionHeaders(List<List<String>> rows) {
        return rows.stream()
                .limit(20)
                .map(this::findColumns)
                .anyMatch(columns -> columns.containsKey("date") && columns.containsKey("amount"));
    }

    private char detectDelimiter(String text) {
        String firstLine = text.lines().findFirst().orElse("");
        int commas = 0;
        int semicolons = 0;
        int tabs = 0;
        boolean quoted = false;
        for (int index = 0; index < firstLine.length(); index++) {
            char current = firstLine.charAt(index);
            if (current == '"') {
                quoted = !quoted;
            } else if (!quoted) {
                if (current == ',') commas++;
                if (current == ';') semicolons++;
                if (current == '\t') tabs++;
            }
        }
        if (tabs > commas && tabs > semicolons) return '\t';
        return semicolons > commas ? ';' : ',';
    }

    private String toTransactionText(List<List<String>> rows) {
        int headerRowIndex = -1;
        Map<String, Integer> columns = Map.of();
        for (int index = 0; index < Math.min(rows.size(), 20); index++) {
            Map<String, Integer> candidate = findColumns(rows.get(index));
            if (candidate.containsKey("date") && candidate.containsKey("amount")) {
                headerRowIndex = index;
                columns = candidate;
                break;
            }
        }
        if (headerRowIndex < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The spreadsheet must contain date and amount columns.");
        }

        StringBuilder text = new StringBuilder();
        int transactionCount = 0;
        for (int rowIndex = headerRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
            if (transactionCount >= MAX_ROWS) {
                throw new ResponseStatusException(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "The spreadsheet exceeds the 10,000 transaction limit.");
            }
            List<String> row = rows.get(rowIndex);
            String date = value(row, columns, "date");
            String amount = value(row, columns, "amount");
            if (date.isBlank() || amount.isBlank()) {
                continue;
            }
            text.append("Date: ").append(normalizeDate(date)).append('\n');
            appendField(text, "Description", value(row, columns, "description"));
            appendField(text, "Merchant", value(row, columns, "merchant"));
            appendField(text, "Category", value(row, columns, "category"));
            appendField(text, "Payment method", value(row, columns, "payment"));
            appendField(text, "Transaction ID", value(row, columns, "reference"));
            String type = value(row, columns, "type").toLowerCase(Locale.ROOT);
            if (type.contains("income") || type.contains("credit") || type.contains("received")) {
                text.append("Payment received\n");
            } else if (type.contains("expense") || type.contains("debit") || type.contains("paid")) {
                text.append("Payment made\n");
            }
            text.append("Amount: ").append(amount).append("\n\n");
            transactionCount++;
        }
        if (transactionCount == 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "No rows with both a date and amount were found in the spreadsheet.");
        }
        return text.toString();
    }

    private Map<String, Integer> findColumns(List<String> header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < header.size(); index++) {
            String normalized = header.get(index)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9_]", "");
            for (Map.Entry<String, Set<String>> field : FIELD_HEADERS.entrySet()) {
                if (field.getValue().contains(normalized)) {
                    columns.putIfAbsent(field.getKey(), index);
                }
            }
        }
        return columns;
    }

    private String value(List<String> row, Map<String, Integer> columns, String field) {
        Integer index = columns.get(field);
        return index == null || index >= row.size() ? "" : row.get(index).trim();
    }

    private String normalizeDate(String value) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value.trim(), format).toString();
            } catch (DateTimeParseException ignored) {
                // Preserve unfamiliar but readable date values for the transaction parser.
            }
        }
        return value;
    }

    private void appendField(StringBuilder text, String label, String value) {
        if (!value.isBlank()) {
            text.append(label).append(": ").append(value).append('\n');
        }
    }

    private ResponseStatusException invalidTabularFile() {
        return new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "The CSV file has an invalid quoted field.");
    }
}
