package com.fintech.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.transactionservice.dto.request.TransactionRequest;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.entity.TransactionStatus;
import com.fintech.transactionservice.exception.TransactionNotFoundException;
import com.fintech.transactionservice.service.IdempotencyService;
import com.fintech.transactionservice.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private IdempotencyService idempotencyService;

    private Transaction buildTransaction(String txnId, TransactionStatus status) {
        Transaction txn = new Transaction(txnId, "PAY-001", "user-1", "ACC-FROM", "ACC-TO",
                new BigDecimal("100.00"), "Test transaction");
        txn.setStatus(status);
        txn.setCreatedAt(Instant.now());
        txn.setUpdatedAt(Instant.now());
        return txn;
    }

    // --- POST /api/transactions ---

    @Test
    @DisplayName("POST /api/transactions - success returns 201")
    void createTransaction_success() throws Exception {
        TransactionRequest request = new TransactionRequest("user-1", "ACC-FROM", "ACC-TO",
                new BigDecimal("100.00"), "Payment");
        Transaction txn = buildTransaction("TXN-001", TransactionStatus.PENDING);

        when(idempotencyService.checkDuplicate("key-123")).thenReturn(Optional.empty());
        when(transactionService.initiateTransaction(any(TransactionRequest.class), eq("key-123")))
                .thenReturn(txn);

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/transactions - duplicate idempotency key returns 409")
    void createTransaction_duplicateIdempotencyKey_returnsConflict() throws Exception {
        TransactionRequest request = new TransactionRequest("user-1", "ACC-FROM", "ACC-TO",
                new BigDecimal("100.00"), "Payment");
        Transaction existing = buildTransaction("TXN-EXISTING", TransactionStatus.COMPLETED);

        when(idempotencyService.checkDuplicate("dup-key")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "dup-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.transactionId").value("TXN-EXISTING"));
    }

    @Test
    @DisplayName("POST /api/transactions - missing idempotency key returns 400")
    void createTransaction_missingIdempotencyKey_returnsBadRequest() throws Exception {
        TransactionRequest request = new TransactionRequest("user-1", "ACC-FROM", "ACC-TO",
                new BigDecimal("100.00"), "Payment");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/transactions - validation error for missing fields returns 400")
    void createTransaction_validationError_returnsBadRequest() throws Exception {
        // Missing required fields
        TransactionRequest request = new TransactionRequest();

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-456")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/transactions/{id} ---

    @Test
    @DisplayName("GET /api/transactions/{id} - found returns 200")
    void getTransaction_found() throws Exception {
        Transaction txn = buildTransaction("TXN-002", TransactionStatus.COMPLETED);
        when(transactionService.findById("TXN-002")).thenReturn(Optional.of(txn));

        mockMvc.perform(get("/api/transactions/TXN-002")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-002"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/transactions/{id} - not found returns 404")
    void getTransaction_notFound() throws Exception {
        when(transactionService.findById("TXN-NONE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/transactions/TXN-NONE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/transactions ---

    @Test
    @DisplayName("GET /api/transactions - returns list of transactions")
    void getTransactions_returnsList() throws Exception {
        Transaction txn1 = buildTransaction("TXN-A", TransactionStatus.COMPLETED);
        Transaction txn2 = buildTransaction("TXN-B", TransactionStatus.PENDING);
        Page<Transaction> page = new PageImpl<>(List.of(txn1, txn2));

        when(transactionService.findTransactions(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/transactions?accountId=ACC-1 - filters by account")
    void getTransactions_filterByAccount() throws Exception {
        Transaction txn = buildTransaction("TXN-C", TransactionStatus.COMPLETED);
        Page<Transaction> page = new PageImpl<>(List.of(txn));

        when(transactionService.findTransactions(eq("ACC-1"), eq(null), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", "ACC-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/transactions?status=INVALID - invalid status returns 400")
    void getTransactions_invalidStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("status", "INVALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/transactions/{id}/status ---

    @Test
    @DisplayName("GET /api/transactions/{id}/status - returns status info")
    void getTransactionStatus_found() throws Exception {
        Transaction txn = buildTransaction("TXN-003", TransactionStatus.PROCESSING);
        when(transactionService.findById("TXN-003")).thenReturn(Optional.of(txn));

        mockMvc.perform(get("/api/transactions/TXN-003/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-003"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("GET /api/transactions/{id}/status - not found returns 404")
    void getTransactionStatus_notFound() throws Exception {
        when(transactionService.findById("TXN-GONE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/transactions/TXN-GONE/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
