package com.fintech.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.transactionservice.dto.request.ReversalRequest;
import com.fintech.transactionservice.entity.Reversal;
import com.fintech.transactionservice.entity.ReversalStatus;
import com.fintech.transactionservice.entity.ReversalType;
import com.fintech.transactionservice.exception.InvalidTransactionException;
import com.fintech.transactionservice.exception.TransactionNotFoundException;
import com.fintech.transactionservice.service.DisputeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReversalController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReversalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DisputeService disputeService;

    private Reversal buildReversal(String id, ReversalStatus status) {
        Reversal reversal = new Reversal(id, "TXN-ORIG-001", ReversalType.FULL_REFUND,
                new BigDecimal("100.00"), "Customer requested refund");
        reversal.setStatus(status);
        reversal.setReversalTransactionId("TXN-REV-001");
        reversal.setCreatedAt(Instant.now());
        reversal.setUpdatedAt(Instant.now());
        return reversal;
    }

    // --- POST /api/reversals ---

    @Test
    @DisplayName("POST /api/reversals - success returns 201")
    void initiateReversal_success() throws Exception {
        ReversalRequest request = new ReversalRequest();
        request.setOriginalTransactionId("TXN-ORIG-001");
        request.setReversalType(ReversalType.FULL_REFUND);
        request.setAmount(new BigDecimal("100.00"));
        request.setReason("Customer requested refund");

        Reversal reversal = buildReversal("REV-001", ReversalStatus.COMPLETED);
        reversal.setCompletedAt(Instant.now());

        when(disputeService.initiateReversal(any(ReversalRequest.class))).thenReturn(reversal);

        mockMvc.perform(post("/api/reversals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("REV-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.originalTransactionId").value("TXN-ORIG-001"));
    }

    @Test
    @DisplayName("POST /api/reversals - validation error returns 400")
    void initiateReversal_validationError() throws Exception {
        ReversalRequest request = new ReversalRequest();
        // Missing required fields

        mockMvc.perform(post("/api/reversals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reversals - transaction not found returns 404")
    void initiateReversal_transactionNotFound() throws Exception {
        ReversalRequest request = new ReversalRequest();
        request.setOriginalTransactionId("TXN-NOT-EXIST");
        request.setReversalType(ReversalType.FULL_REFUND);
        request.setAmount(new BigDecimal("100.00"));
        request.setReason("Refund");

        when(disputeService.initiateReversal(any(ReversalRequest.class)))
                .thenThrow(new TransactionNotFoundException("TXN-NOT-EXIST"));

        mockMvc.perform(post("/api/reversals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/reversals - duplicate reversal returns 400")
    void initiateReversal_duplicateReversal() throws Exception {
        ReversalRequest request = new ReversalRequest();
        request.setOriginalTransactionId("TXN-ORIG-001");
        request.setReversalType(ReversalType.FULL_REFUND);
        request.setAmount(new BigDecimal("100.00"));
        request.setReason("Refund");

        when(disputeService.initiateReversal(any(ReversalRequest.class)))
                .thenThrow(new InvalidTransactionException("Reversal already exists for this transaction"));

        mockMvc.perform(post("/api/reversals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/reversals/{id} ---

    @Test
    @DisplayName("GET /api/reversals/{id} - found returns 200")
    void getReversal_found() throws Exception {
        Reversal reversal = buildReversal("REV-002", ReversalStatus.COMPLETED);
        when(disputeService.getReversal("REV-002")).thenReturn(reversal);

        mockMvc.perform(get("/api/reversals/REV-002")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("REV-002"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reversalType").value("FULL_REFUND"));
    }

    @Test
    @DisplayName("GET /api/reversals/{id} - not found returns 400")
    void getReversal_notFound() throws Exception {
        when(disputeService.getReversal("REV-NONE"))
                .thenThrow(new InvalidTransactionException("Reversal not found: REV-NONE"));

        mockMvc.perform(get("/api/reversals/REV-NONE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/reversals ---

    @Test
    @DisplayName("GET /api/reversals - returns all reversals")
    void getReversals_all() throws Exception {
        Reversal r1 = buildReversal("REV-A", ReversalStatus.COMPLETED);
        Reversal r2 = buildReversal("REV-B", ReversalStatus.INITIATED);
        Page<Reversal> page = new PageImpl<>(List.of(r1, r2));

        when(disputeService.getAllReversals(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/reversals")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/reversals?transactionId=TXN-001 - filters by transaction")
    void getReversals_filterByTransaction() throws Exception {
        Reversal reversal = buildReversal("REV-C", ReversalStatus.COMPLETED);
        when(disputeService.getReversalsByTransaction("TXN-001")).thenReturn(List.of(reversal));

        mockMvc.perform(get("/api/reversals")
                        .param("transactionId", "TXN-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
