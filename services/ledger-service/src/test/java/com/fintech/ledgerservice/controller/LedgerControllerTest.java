package com.fintech.ledgerservice.controller;

import com.fintech.ledgerservice.dto.response.AccountBalanceResponse;
import com.fintech.ledgerservice.entity.LedgerEntry;
import com.fintech.ledgerservice.entity.LedgerEntryType;
import com.fintech.ledgerservice.service.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LedgerController.class)
@AutoConfigureMockMvc(addFilters = false)
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LedgerService ledgerService;

    @Test
    @DisplayName("GET /api/ledger/balance/{accountId} returns account balance")
    void getBalance_returnsAccountBalance() throws Exception {
        String accountId = "ACC-001";
        AccountBalanceResponse response = new AccountBalanceResponse(
                null, accountId, new BigDecimal("1500.00"),
                new BigDecimal("1500.00"), BigDecimal.ZERO, "USD");

        when(ledgerService.getAccountBalance(accountId)).thenReturn(response);

        mockMvc.perform(get("/api/ledger/balance/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(accountId)))
                .andExpect(jsonPath("$.currentBalance", is(1500.00)))
                .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    @DisplayName("GET /api/ledger/entries?accountId={id} returns entries for account")
    void getEntries_withAccountId_returnsEntriesForAccount() throws Exception {
        String accountId = "ACC-001";
        LedgerEntry entry = new LedgerEntry("E1", "TXN-001", "PAY-001",
                accountId, LedgerEntryType.CREDIT, new BigDecimal("500.00"), "Payment received");

        when(ledgerService.getEntriesByAccountId(accountId)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/ledger/entries").param("accountId", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountNumber", is(accountId)))
                .andExpect(jsonPath("$[0].entryType", is("CREDIT")))
                .andExpect(jsonPath("$[0].amount", is(500.00)));
    }

    @Test
    @DisplayName("GET /api/ledger/entries without accountId returns paginated entries")
    void getEntries_withoutAccountId_returnsPaginatedEntries() throws Exception {
        LedgerEntry entry = new LedgerEntry("E1", "TXN-001", "PAY-001",
                "ACC-001", LedgerEntryType.DEBIT, new BigDecimal("200.00"), "Transfer out");
        Page<LedgerEntry> page = new PageImpl<>(List.of(entry));

        when(ledgerService.getAllEntries(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/ledger/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].txnId", is("TXN-001")));
    }

    @Test
    @DisplayName("GET /api/ledger/entries/{transactionId} returns entries for transaction")
    void getEntriesByTransaction_returnsEntries() throws Exception {
        String txnId = "TXN-001";
        LedgerEntry debit = new LedgerEntry("E1", txnId, "PAY-001",
                "ACC-001", LedgerEntryType.DEBIT, new BigDecimal("100.00"), "Transfer");
        LedgerEntry credit = new LedgerEntry("E2", txnId, "PAY-001",
                "ACC-002", LedgerEntryType.CREDIT, new BigDecimal("100.00"), "Transfer");

        when(ledgerService.getEntriesByTransactionId(txnId)).thenReturn(List.of(debit, credit));

        mockMvc.perform(get("/api/ledger/entries/{transactionId}", txnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].entryType", is("DEBIT")))
                .andExpect(jsonPath("$[1].entryType", is("CREDIT")));
    }

    @Test
    @DisplayName("GET /api/ledger/entries/{transactionId} returns 404 when no entries found")
    void getEntriesByTransaction_notFound() throws Exception {
        when(ledgerService.getEntriesByTransactionId("UNKNOWN")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/ledger/entries/{transactionId}", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/ledger/reconcile/{transactionId} returns reconciliation result")
    void reconcileTransaction_returnsResult() throws Exception {
        String txnId = "TXN-001";
        when(ledgerService.reconcile(txnId)).thenReturn(true);

        mockMvc.perform(post("/api/ledger/reconcile/{transactionId}", txnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is(txnId)))
                .andExpect(jsonPath("$.balanced", is(true)))
                .andExpect(jsonPath("$.status", is("RECONCILED")));
    }

    @Test
    @DisplayName("POST /api/ledger/reconcile/{transactionId} returns IMBALANCED when not balanced")
    void reconcileTransaction_imbalanced() throws Exception {
        String txnId = "TXN-002";
        when(ledgerService.reconcile(txnId)).thenReturn(false);

        mockMvc.perform(post("/api/ledger/reconcile/{transactionId}", txnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced", is(false)))
                .andExpect(jsonPath("$.status", is("IMBALANCED")));
    }

    @Test
    @DisplayName("GET /api/ledger/statement returns entries within date range")
    void getAccountStatement_returnsEntries() throws Exception {
        String accountId = "ACC-001";
        LedgerEntry entry = new LedgerEntry("E1", "TXN-001", "PAY-001",
                accountId, LedgerEntryType.CREDIT, new BigDecimal("750.00"), "Salary");

        when(ledgerService.getAccountStatement(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/ledger/statement")
                        .param("accountId", accountId)
                        .param("from", "2024-01-01")
                        .param("to", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount", is(750.00)));
    }
}
