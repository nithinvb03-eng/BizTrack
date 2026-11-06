package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.CreditEntryResponse;
import com.sathya.co.biztrack.dto.CreditEmailRequest;
import com.sathya.co.biztrack.dto.CreditReceiptListItem;
import com.sathya.co.biztrack.dto.CreditReceiptResponse;
import com.sathya.co.biztrack.entity.Business;
import com.sathya.co.biztrack.entity.CreditEntry;
import com.sathya.co.biztrack.entity.CreditReceipt;
import com.sathya.co.biztrack.entity.CreditReceiptLine;
import com.sathya.co.biztrack.entity.CreditTaker;
import com.sathya.co.biztrack.entity.Role;
import com.sathya.co.biztrack.entity.User;
import com.sathya.co.biztrack.repository.BusinessRepository;
import com.sathya.co.biztrack.repository.CreditDateAggregate;
import com.sathya.co.biztrack.repository.CreditEntryRepository;
import com.sathya.co.biztrack.repository.CreditReceiptLineRepository;
import com.sathya.co.biztrack.repository.CreditReceiptRepository;
import com.sathya.co.biztrack.repository.UserRepository;
import com.sathya.co.biztrack.util.CalculationUtil;
import jakarta.mail.MessagingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreditReceiptService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final CreditTakerService takerService;
    private final CreditEntryRepository entries;
    private final CreditReceiptRepository receipts;
    private final CreditReceiptLineRepository receiptLines;
    private final BusinessRepository businesses;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final CreditReceiptPdfService pdfService;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String mailHost;

    public CreditReceiptService(
            CreditTakerService takerService,
            CreditEntryRepository entries,
            CreditReceiptRepository receipts,
            CreditReceiptLineRepository receiptLines,
            BusinessRepository businesses,
            UserRepository users,
            CurrentUser currentUser,
            AuditService audit,
            CreditReceiptPdfService pdfService,
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${MAIL_HOST:}") String mailHost) {
        this.takerService = takerService;
        this.entries = entries;
        this.receipts = receipts;
        this.receiptLines = receiptLines;
        this.businesses = businesses;
        this.users = users;
        this.currentUser = currentUser;
        this.audit = audit;
        this.pdfService = pdfService;
        this.mailSenders = mailSenders;
        this.mailHost = mailHost;
    }

    @Transactional(readOnly = true)
    public CreditReceiptResponse preview(Integer takerId, LocalDate from, LocalDate to) {
        Access access = currentUser.mustBe(Role::canViewReports);
        return createSnapshot(takerId, from, to, access, "Preview", null);
    }

    @Transactional
    public CreditReceiptResponse generate(Integer takerId, LocalDate from, LocalDate to) {
        Access access = currentUser.mustBe(Role::canModify);
        Business business = businesses.findByIdForUpdate(access.businessId())
                .orElseThrow(() -> notFound("Business not found"));
        CreditTaker taker = takerService.findTaker(takerId, access.businessId());
        LocalDateTime generatedAt = LocalDateTime.now(BUSINESS_ZONE);
        String prefix = "CR-" + generatedAt.getYear() + "-";
        long sequence = receipts.countByBusinessIdAndReceiptNoStartingWith(access.businessId(), prefix) + 1;
        String receiptNo = String.format(Locale.ROOT, "%s%04d", prefix, sequence);

        User user = users.findById(access.userId())
                .orElseThrow(() -> notFound("User not found"));
        CreditReceiptResponse snapshot = createSnapshot(
                takerId, from, to, access, receiptNo, generatedAt, business, taker, user);

        CreditReceipt receipt = new CreditReceipt();
        receipt.setBusinessId(access.businessId());
        receipt.setCreditTaker(taker);
        receipt.setReceiptNo(receiptNo);
        receipt.setPeriodFrom(from);
        receipt.setPeriodTo(to);
        receipt.setTotalAmount(snapshot.totalAmount());
        receipt.setEntryCount(snapshot.entryCount());
        receipt.setGeneratedAt(generatedAt);
        receipt.setCreatedBy(access.userId());
        receipt.setBusinessName(snapshot.businessName());
        receipt.setBusinessGstin(snapshot.businessGstin());
        receipt.setGeneratedByName(snapshot.generatedBy());
        receipt.setCreditTakerName(snapshot.creditTakerName());
        receipt.setCreditTakerPhone(snapshot.phone());
        receipt.setCreditTakerEmail(taker.getEmail());
        receipt.setCreditTakerType(snapshot.type());
        receipt = receipts.save(receipt);

        for (CreditReceiptResponse.ReceiptEntry entry : snapshot.entries()) {
            CreditReceiptLine line = new CreditReceiptLine();
            line.setReceipt(receipt);
            line.setBusinessId(access.businessId());
            line.setEntryDate(entry.date());
            line.setEntryTime(entry.time());
            line.setAmount(entry.amount());
            line.setNote(entry.note());
            receiptLines.save(line);
        }
        audit.log(access.businessId(), access.userId(), "CREATE", "CREDIT_RECEIPT", receipt.getId().longValue());
        return withReceiptId(receipt.getId(), snapshot);
    }

    @Transactional(readOnly = true)
    public List<CreditReceiptListItem> list(Integer takerId) {
        Access access = currentUser.mustBe(Role::canViewReports);
        takerService.findTaker(takerId, access.businessId());
        return receipts
                .findByBusinessIdAndCreditTaker_IdOrderByGeneratedAtDescIdDesc(access.businessId(), takerId)
                .stream()
                .map(receipt -> new CreditReceiptListItem(
                        receipt.getId(),
                        receipt.getReceiptNo(),
                        receipt.getPeriodFrom(),
                        receipt.getPeriodTo(),
                        receipt.getTotalAmount(),
                        receipt.getEntryCount(),
                        receipt.getGeneratedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Integer receiptId) {
        Access access = currentUser.mustBe(Role::canViewReports);
        CreditReceipt receipt = findReceipt(receiptId, access.businessId());
        return pdfService.create(snapshot(receipt));
    }

    @Transactional
    public void email(Integer receiptId, CreditEmailRequest request) {
        Access access = currentUser.mustBe(Role::canModify);
        if (mailHost.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email is not configured");
        }
        JavaMailSender mailSender = mailSenders.getIfAvailable();
        if (mailSender == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email is not configured");
        }

        CreditReceipt receipt = findReceipt(receiptId, access.businessId());
        CreditReceiptResponse snapshot = snapshot(receipt);
        String recipient = request.toEmail();
        if (recipient == null || recipient.isBlank()) {
            recipient = receipt.getCreditTakerEmail();
        }
        if (recipient == null || recipient.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide an email address");
        }

        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject("Credit receipt " + receipt.getReceiptNo());
            helper.setText("Please find the credit receipt attached.");
            helper.addAttachment(
                    receipt.getReceiptNo() + ".pdf",
                    new ByteArrayResource(pdfService.create(snapshot)),
                    "application/pdf");
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "The receipt email could not be sent.", exception);
        }
    }

    private CreditReceiptResponse createSnapshot(
            Integer takerId,
            LocalDate from,
            LocalDate to,
            Access access,
            String receiptNo,
            LocalDateTime generatedAt) {
        Business business = businesses.findById(access.businessId())
                .orElseThrow(() -> notFound("Business not found"));
        CreditTaker taker = takerService.findTaker(takerId, access.businessId());
        User user = users.findById(access.userId())
                .orElseThrow(() -> notFound("User not found"));
        return createSnapshot(takerId, from, to, access, receiptNo, generatedAt, business, taker, user);
    }

    private CreditReceiptResponse createSnapshot(
            Integer takerId,
            LocalDate from,
            LocalDate to,
            Access access,
            String receiptNo,
            LocalDateTime generatedAt,
            Business business,
            CreditTaker taker,
            User user) {
        if (from == null || to == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose both a start date and an end date");
        }
        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
        }
        List<CreditEntry> periodEntries =
                entries.findByBusinessIdAndCreditTaker_IdAndEntryDateBetweenOrderByEntryDateAscEntryTimeAscIdAsc(
                        access.businessId(), takerId, from, to);
        List<CreditDateAggregate> aggregates =
                entries.totalsByDate(access.businessId(), takerId, from, to);
        BigDecimal total = CalculationUtil.sum(aggregates.stream()
                .map(CreditDateAggregate::getTotal)
                .toList());
        List<CreditReceiptResponse.ReceiptEntry> receiptEntries = periodEntries.stream()
                .map(entry -> new CreditReceiptResponse.ReceiptEntry(
                        entry.getEntryDate(), entry.getEntryTime(), entry.getAmount(), entry.getNote()))
                .toList();
        return new CreditReceiptResponse(
                null,
                business.getBusinessName(),
                business.getGstin(),
                receiptNo,
                generatedAt == null ? LocalDateTime.now(BUSINESS_ZONE) : generatedAt,
                user.getFullName(),
                taker.getId(),
                taker.getName(),
                taker.getPhone(),
                taker.getType(),
                from,
                to,
                receiptEntries,
                (long) periodEntries.size(),
                total.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private CreditReceipt findReceipt(Integer receiptId, Integer businessId) {
        return receipts.findByIdAndBusinessId(receiptId, businessId)
                .orElseThrow(() -> notFound("Credit receipt not found"));
    }

    private CreditReceiptResponse snapshot(CreditReceipt receipt) {
        List<CreditReceiptResponse.ReceiptEntry> lines = receiptLines
                .findByReceipt_IdOrderByEntryDateAscEntryTimeAscIdAsc(receipt.getId())
                .stream()
                .map(line -> new CreditReceiptResponse.ReceiptEntry(
                        line.getEntryDate(), line.getEntryTime(), line.getAmount(), line.getNote()))
                .toList();
        return new CreditReceiptResponse(
                receipt.getId(),
                receipt.getBusinessName(),
                receipt.getBusinessGstin(),
                receipt.getReceiptNo(),
                receipt.getGeneratedAt(),
                receipt.getGeneratedByName(),
                receipt.getCreditTaker().getId(),
                receipt.getCreditTakerName(),
                receipt.getCreditTakerPhone(),
                receipt.getCreditTakerType(),
                receipt.getPeriodFrom(),
                receipt.getPeriodTo(),
                lines,
                receipt.getEntryCount(),
                receipt.getTotalAmount());
    }

    private CreditReceiptResponse withReceiptId(Integer receiptId, CreditReceiptResponse snapshot) {
        return new CreditReceiptResponse(
                receiptId,
                snapshot.businessName(),
                snapshot.businessGstin(),
                snapshot.receiptNo(),
                snapshot.generatedAt(),
                snapshot.generatedBy(),
                snapshot.creditTakerId(),
                snapshot.creditTakerName(),
                snapshot.phone(),
                snapshot.type(),
                snapshot.periodFrom(),
                snapshot.periodTo(),
                snapshot.entries(),
                snapshot.entryCount(),
                snapshot.totalAmount());
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
