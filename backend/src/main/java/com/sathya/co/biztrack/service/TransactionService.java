package com.sathya.co.biztrack.service;

import com.sathya.co.biztrack.dto.TransactionRequest;
import com.sathya.co.biztrack.dto.ReceiptDownload;
import com.sathya.co.biztrack.entity.*;
import com.sathya.co.biztrack.repository.*;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

    @Autowired private TransactionRepository repo;
    @Autowired private CategoryRepository categories;
    @Autowired private CurrentUser currentUser;
    @Autowired private AuditService audit;
    @Autowired private TransactionReceiptRepository receipts;

    /** Ledger screen: date range + optional type, category and search text. */
    public List<Transaction> list(LocalDate from, LocalDate to, TransactionType type,
                                  Integer categoryId, String q) {
        Access a = currentUser.get();
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.withDayOfMonth(1);   // default: this month so far
        String needle = (q == null) ? "" : q.trim().toLowerCase();

        return repo.findByBusinessIdAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(
                        a.businessId(), start, end)
                .stream()
                .filter(t -> type == null || t.getTransactionType() == type)
                .filter(t -> categoryId == null || t.getCategoryId().equals(categoryId))
                .filter(t -> needle.isEmpty()
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(needle)))
                .toList();
    }

    @Transactional
    public Transaction add(TransactionRequest r) {
        Access a = currentUser.mustBe(Role::canAdd);            // owner or employee
        return create(r, a);
    }

    @Transactional
    public Transaction addWithReceipt(TransactionRequest r, String fileName, String contentType, byte[] content) {
        Access a = currentUser.mustBe(Role::canAdd);
        Transaction saved = create(r, a);
        TransactionReceipt receipt = new TransactionReceipt();
        receipt.setTransactionId(saved.getId());
        receipt.setBusinessId(a.businessId());
        receipt.setFileName(fileName);
        receipt.setContentType(contentType);
        receipt.setContent(content);
        receipts.save(receipt);
        return saved;
    }

    private Transaction create(TransactionRequest r, Access a) {
        Transaction t = new Transaction();
        t.setBusinessId(a.businessId());                        // from the session, never from the client
        t.setCreatedBy(a.userId());
        copy(r, t, a.businessId());
        Transaction saved = repo.save(t);
        audit.log(a.businessId(), a.userId(), "CREATE", "TRANSACTION", saved.getId());
        return saved;
    }

    @Transactional
    public Transaction update(Long id, TransactionRequest r) {
        Access a = currentUser.mustBe(Role::canModify);         // owner only
        Transaction t = find(id, a.businessId());
        copy(r, t, a.businessId());
        Transaction saved = repo.save(t);
        audit.log(a.businessId(), a.userId(), "UPDATE", "TRANSACTION", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Access a = currentUser.mustBe(Role::canModify);         // owner only
        Transaction transaction = find(id, a.businessId());
        receipts.deleteByTransactionIdAndBusinessId(id, a.businessId());
        repo.delete(transaction);
        audit.log(a.businessId(), a.userId(), "DELETE", "TRANSACTION", id);
    }

    @Transactional(readOnly = true)
    public ReceiptDownload getReceipt(Long id) {
        Access a = currentUser.get();
        find(id, a.businessId());
        TransactionReceipt receipt = receipts.findByTransactionIdAndBusinessId(id, a.businessId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
        return new ReceiptDownload(receipt.getFileName(), receipt.getContentType(), receipt.getContent());
    }

    private Transaction find(Long id, Integer businessId) {
        return repo.findByIdAndBusinessId(id, businessId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private void copy(TransactionRequest r, Transaction t, Integer businessId) {
        Category c = categories.findByIdAndBusinessId(r.categoryId(), businessId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category"));
        if (c.getTransactionType() != r.transactionType()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Category '" + c.getCategoryName() + "' is not an " + r.transactionType() + " category");
        }
        t.setTransactionType(r.transactionType());
        t.setCategoryId(c.getId());
        t.setAmount(r.amount().setScale(2, RoundingMode.HALF_UP));
        t.setTransactionDate(r.transactionDate());
        t.setTransactionTime(r.transactionTime());
        t.setDescription(r.description().trim());
        t.setPaymentMethod(r.paymentMethod());
    }
}