package com.fintech.ledgerservice.controller;

import com.fintech.ledgerservice.dto.response.AccountBalanceResponse;
import com.fintech.ledgerservice.entity.LedgerEntry;
import com.fintech.ledgerservice.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private static final Logger logger = LoggerFactory.getLogger(LedgerController.class);

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * GET /api/ledger/entries?accountId={id}
     * Returns all ledger entries for the given account.
     */
    @GetMapping("/entries")
    public ResponseEntity<List<LedgerEntry>> getEntriesByAccount(@RequestParam String accountId) {
        logger.info("GET /api/ledger/entries?accountId={}", accountId);
        List<LedgerEntry> entries = ledgerService.getEntriesByAccountId(accountId);
        return ResponseEntity.ok(entries);
    }

    /**
     * GET /api/ledger/entries/{transactionId}
     * Returns all ledger entries for a specific transaction.
     */
    @GetMapping("/entries/{transactionId}")
    public ResponseEntity<List<LedgerEntry>> getEntriesByTransaction(@PathVariable String transactionId) {
        logger.info("GET /api/ledger/entries/{}", transactionId);
        List<LedgerEntry> entries = ledgerService.getEntriesByTransactionId(transactionId);
        if (entries.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(entries);
    }

    /**
     * GET /api/ledger/balance/{accountId}
     * Returns the computed balance for an account (sum(CREDIT) - sum(DEBIT)).
     */
    @GetMapping("/balance/{accountId}")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(@PathVariable String accountId) {
        logger.info("GET /api/ledger/balance/{}", accountId);
        AccountBalanceResponse balance = ledgerService.getAccountBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    /**
     * GET /api/ledger/statement?accountId=&from=&to=
     * Returns an account statement (entries within date range).
     */
    @GetMapping("/statement")
    public ResponseEntity<List<LedgerEntry>> getAccountStatement(
            @RequestParam String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        logger.info("GET /api/ledger/statement?accountId={}&from={}&to={}", accountId, from, to);

        Instant startDate = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endDate = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<LedgerEntry> statement = ledgerService.getAccountStatement(accountId, startDate, endDate);
        return ResponseEntity.ok(statement);
    }

    /**
     * POST /api/ledger/reconcile/{transactionId}
     * Reconciles a transaction and returns whether it is balanced.
     */
    @PostMapping("/reconcile/{transactionId}")
    public ResponseEntity<Map<String, Object>> reconcileTransaction(@PathVariable String transactionId) {
        logger.info("POST /api/ledger/reconcile/{}", transactionId);

        boolean balanced = ledgerService.reconcile(transactionId);

        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", transactionId);
        response.put("balanced", balanced);
        response.put("status", balanced ? "RECONCILED" : "IMBALANCED");

        return ResponseEntity.ok(response);
    }
}
