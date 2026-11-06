package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.ExtractedTransaction;
import com.sathya.co.biztrack.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ReceiptTransactionParser {

    private static final Pattern DATE = Pattern.compile(
            "\\b(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{1,2}\\s+[A-Za-z]{3,9},?\\s+\\d{4})\\b");
    private static final Pattern AMOUNT = Pattern.compile(
            "(?:(?:₹|INR\\b|Rs\\.?\\s*)\\s*)?([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REFERENCE = Pattern.compile(
            "(?i)\\b(?:transaction\\s*(?:id|no|number)|txn\\s*id|reference\\s*(?:no|number)?|ref\\s*(?:no|number)?)\\s*[:#-]?\\s*([A-Z0-9-]{4,})");
    private static final Pattern RECEIPT_REFERENCE = Pattern.compile(
            "(?i)\\b(?:receipt|invoice)\\s*(?:no|number|#)\\s*[:#-]?\\s*([A-Z0-9-]{3,})");
    private static final Pattern TIME = Pattern.compile(
            "\\b((?:0?[1-9]|1[0-2]):[0-5]\\d(?::[0-5]\\d)?\\s*[AP]M|(?:[01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TAX = Pattern.compile(
            "(?i)\\b(?:gst|tax|cgst|sgst|igst)\\b[^\\d₹]{0,20}(?:₹|INR\\b|Rs\\.?\\s*)?([\\d,]+(?:\\.\\d{1,2})?)");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d.M.uuuu"),
            DateTimeFormatter.ofPattern("d/M/uu"),
            DateTimeFormatter.ofPattern("d-M-uu"),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH));

    public List<ExtractedTransaction> parse(String text) {
        List<ExtractedTransaction> rows = new ArrayList<>();
        for (String page : text.split("\\f")) {
            List<ExtractedTransaction> pageRows = new ArrayList<>();
            String[] lines = page.split("\\R");
            List<Integer> datedLines = new ArrayList<>();
            for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
                String line = lines[lineNumber];
                if (findDate(line) != null) {
                    datedLines.add(lineNumber);
                }
                LocalDate date = findDate(line);
                BigDecimal amount = findAmount(line, false);
                if (date != null && amount != null) {
                    pageRows.add(createTransaction(lines[lineNumber], date, amount, page));
                }
            }
            if (!pageRows.isEmpty()) {
                rows.addAll(pageRows);
                continue;
            }

            if (datedLines.size() > 1) {
                for (int index = 0; index < datedLines.size(); index++) {
                    int start = datedLines.get(index);
                    int end = index + 1 < datedLines.size() ? datedLines.get(index + 1) : lines.length;
                    String section = String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));
                    LocalDate date = findDate(section);
                    BigDecimal amount = findAmount(section, true);
                    if (date != null || amount != null) {
                        pageRows.add(createTransaction(section, date, amount, page));
                    }
                }
                if (!pageRows.isEmpty()) {
                    rows.addAll(pageRows);
                    continue;
                }
            }

            LocalDate date = findDate(page);
            BigDecimal amount = findAmount(page, true);
            if (date != null || amount != null) {
                rows.add(createTransaction(page, date, amount, page));
            }
        }
        if (!rows.isEmpty()) {
            return rows;
        }

        if (firstMerchant(text) == null) {
            return List.of();
        }
        return List.of(createTransaction(text, null, null, text));
    }

    private ExtractedTransaction createTransaction(
            String source, LocalDate date, BigDecimal amount, String documentText) {
        String merchant = firstMerchant(documentText);
        String currency = hasInrCurrency(source) || hasInrCurrency(documentText) ? "INR" : null;
        String paymentMethod = findPaymentMethod(source);
        if (paymentMethod == null) {
            paymentMethod = findPaymentMethod(documentText);
        }
        Matcher referenceMatcher = REFERENCE.matcher(source);
        String reference = referenceMatcher.find() ? referenceMatcher.group(1) : null;
        if (reference == null) {
            referenceMatcher = REFERENCE.matcher(documentText);
            reference = referenceMatcher.find() ? referenceMatcher.group(1) : null;
        }
        Matcher receiptReferenceMatcher = RECEIPT_REFERENCE.matcher(documentText);
        String receiptReference = receiptReferenceMatcher.find() ? receiptReferenceMatcher.group(1) : null;
        BigDecimal tax = findTax(documentText);
        String description = findLabeledValue(source, "description");
        if (description == null) {
            description = findLabeledValue(documentText, "description");
        }
        if (description == null && !source.equals(documentText)) {
            description = cleanDescription(source, merchant);
        }
        TransactionType type = transactionType(source);
        if (type == null) {
            type = transactionType(documentText);
        }
        LocalTime time = findTime(source);
        if (time == null) {
            time = findTime(documentText);
        }
        String category = findLabeledValue(source, "category");
        if (category == null) {
            category = findLabeledValue(documentText, "category");
        }
        BigDecimal confidence = BigDecimal.valueOf(
                (amount != null ? 0.45 : 0.0) + (date != null ? 0.30 : 0.0) +
                        (merchant != null ? 0.15 : 0.0) + (paymentMethod != null ? 0.10 : 0.0))
                .setScale(2);
        return new ExtractedTransaction(
                merchant, amount, currency, date, time, description, category,
                paymentMethod, reference, tax,
                receiptReference, type, confidence);
    }

    private LocalDate findDate(String text) {
        Matcher matcher = DATE.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).replaceAll(",", "");
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, format);
            } catch (DateTimeParseException ignored) {
                // Try the next unambiguous date format.
            }
        }
        return null;
    }

    private BigDecimal findAmount(String text, boolean preferTotal) {
        if (preferTotal) {
            Matcher preferredTotal = Pattern.compile(
                    "(?i)\\b(?:grand\\s+total|total\\s+(?:amount|paid)|amount\\s+paid|net\\s+amount|paid)\\b[^\\d]{0,24}(?:₹|INR\\b|Rs\\.?\\s*)?([\\d,]+(?:\\.\\d{1,2})?)")
                    .matcher(text);
            if (preferredTotal.find()) {
                BigDecimal value = decimal(preferredTotal.group(1));
                if (value != null && value.signum() > 0) {
                    return value;
                }
            }
        }

        Matcher labeledAmount = Pattern.compile(
                "(?i)\\b(?:amount|total)\\b[^\\d]{0,24}(?:₹|INR\\b|Rs\\.?\\s*)?([\\d,]+(?:\\.\\d{1,2})?)")
                .matcher(text);
        if (labeledAmount.find()) {
            BigDecimal value = decimal(labeledAmount.group(1));
            if (value != null && value.signum() > 0) {
                return value;
            }
        }

        Matcher matcher = AMOUNT.matcher(text);
        BigDecimal first = null;
        BigDecimal last = null;
        while (matcher.find()) {
            String matched = matcher.group();
            if (!matched.contains(".") && !matched.matches("(?i).*?(₹|INR|Rs).*")) {
                continue;
            }
            BigDecimal value = decimal(matcher.group(1));
            if (value == null || value.signum() <= 0) {
                continue;
            }
            if (first == null) {
                first = value;
            }
            last = value;
        }
        return last == null ? first : last;
    }

    private BigDecimal findTax(String text) {
        Matcher matcher = TAX.matcher(text);
        return matcher.find() ? decimal(matcher.group(1)) : null;
    }

    private LocalTime findTime(String text) {
        Matcher matcher = TIME.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).replaceAll("\\s+", " ").toUpperCase(Locale.ENGLISH);
        List<DateTimeFormatter> formats = value.endsWith("AM") || value.endsWith("PM")
                ? List.of(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
                        DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH))
                : List.of(DateTimeFormatter.ofPattern("H:mm"), DateTimeFormatter.ofPattern("H:mm:ss"));
        for (DateTimeFormatter format : formats) {
            try {
                return LocalTime.parse(value, format);
            } catch (DateTimeParseException ignored) {
                // Try the next supported clock format.
            }
        }
        return null;
    }

    private String findLabeledValue(String text, String label) {
        Pattern pattern = Pattern.compile(
                "(?im)^\\s*" + Pattern.quote(label) + "\\s*[:#-]\\s*(.{2,80})\\s*$");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value.replace(",", "")).setScale(2);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String firstMerchant(String text) {
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replaceAll("\\s+", " ").trim();
            Matcher merchant = Pattern.compile("(?i)^merchant\\s*[:#-]\\s*(.+)$").matcher(line);
            if (merchant.matches()) {
                return merchant.group(1).trim();
            }
            if (line.length() >= 3 && line.length() <= 80 &&
                    !line.matches("(?i)^(?:(?:tax\\s+)?invoice|receipt|bill|date|total|amount|description|category|payment|merchant|gstin|transaction\\s*(?:id|no|number))\\b.*") &&
                    !line.matches("(?i)^(?:www\\.|https?://).*")) {
                return line;
            }
        }
        return null;
    }

    private String cleanDescription(String source, String merchant) {
        String line = source.replaceAll("\\s+", " ").trim();
        if (merchant != null) {
            line = line.replace(merchant, "").trim();
        }
        line = DATE.matcher(line).replaceAll("").trim();
        line = AMOUNT.matcher(line).replaceAll("").replaceAll("\\s+", " ").trim();
        return line.isBlank() ? null : line;
    }

    private String findPaymentMethod(String text) {
        if (text.matches("(?is).*(\\bupi\\b|\\bbhim\\b|\\bgpay\\b|google\\s*pay|phonepe|paytm).*")) return "UPI";
        if (text.matches("(?is).*\\bcash\\b.*")) return "Cash";
        if (text.matches("(?is).*(\\bcredit\\s*card\\b|\\bvisa\\b|\\bmastercard\\b).*")) return "Credit Card";
        if (text.matches("(?is).*\\bdebit\\s*card\\b.*")) return "Debit Card";
        if (text.matches("(?is).*(\\bneft\\b|\\bimps\\b|\\brtgs\\b|bank\\s+transfer|net\\s*banking).*")) return "Bank Transfer";
        if (text.matches("(?is).*(\\bcheque\\b|\\bcheck\\b).*")) return "Cheque";
        return null;
    }

    private boolean hasInrCurrency(String text) {
        return text.matches("(?is).*?(?:₹|\\bINR\\b|\\bRs\\.?).*?");
    }

    private TransactionType transactionType(String text) {
        if (text.matches(
                "(?is).*\\b(refund|credit\\s+note|payment\\s+received|money\\s+received|deposit)\\b.*")) {
            return TransactionType.INCOME;
        }
        if (text.matches(
                "(?is).*\\b(payment\\s+made|amount\\s+paid|paid|purchase|debit\\s+note)\\b.*")) {
            return TransactionType.EXPENSE;
        }
        return null;
    }
}
