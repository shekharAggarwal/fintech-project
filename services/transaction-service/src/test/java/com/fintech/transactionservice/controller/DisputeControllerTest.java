package com.fintech.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.transactionservice.dto.request.DisputeRequest;
import com.fintech.transactionservice.entity.Dispute;
import com.fintech.transactionservice.entity.DisputeStatus;
import com.fintech.transactionservice.entity.DisputeType;
import com.fintech.transactionservice.exception.InvalidTransactionException;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DisputeController.class)
@AutoConfigureMockMvc(addFilters = false)
class DisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DisputeService disputeService;

    private Dispute buildDispute(String id, DisputeStatus status) {
        Dispute dispute = new Dispute(id, "TXN-001", DisputeType.UNAUTHORIZED, "Unrecognized charge");
        dispute.setStatus(status);
        dispute.setEvidence("screenshot.png");
        dispute.setRefundAmount(new BigDecimal("50.00"));
        dispute.setCreatedAt(Instant.now());
        dispute.setUpdatedAt(Instant.now());
        return dispute;
    }

    // --- POST /api/disputes ---

    @Test
    @DisplayName("POST /api/disputes - success returns 201")
    void openDispute_success() throws Exception {
        DisputeRequest request = new DisputeRequest();
        request.setTransactionId("TXN-001");
        request.setDisputeType(DisputeType.UNAUTHORIZED);
        request.setReason("Unrecognized charge");
        request.setRefundAmount(new BigDecimal("50.00"));

        Dispute dispute = buildDispute("DSP-001", DisputeStatus.OPEN);
        when(disputeService.openDispute(any(DisputeRequest.class))).thenReturn(dispute);

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("DSP-001"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.transactionId").value("TXN-001"));
    }

    @Test
    @DisplayName("POST /api/disputes - validation error returns 400")
    void openDispute_validationError() throws Exception {
        DisputeRequest request = new DisputeRequest();
        // Missing required fields

        mockMvc.perform(post("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/disputes/{id} ---

    @Test
    @DisplayName("GET /api/disputes/{id} - found returns 200")
    void getDispute_found() throws Exception {
        Dispute dispute = buildDispute("DSP-002", DisputeStatus.UNDER_REVIEW);
        when(disputeService.getDispute("DSP-002")).thenReturn(dispute);

        mockMvc.perform(get("/api/disputes/DSP-002")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("DSP-002"))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("GET /api/disputes/{id} - not found returns 400")
    void getDispute_notFound() throws Exception {
        when(disputeService.getDispute("DSP-NONE"))
                .thenThrow(new InvalidTransactionException("Dispute not found: DSP-NONE"));

        mockMvc.perform(get("/api/disputes/DSP-NONE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/disputes ---

    @Test
    @DisplayName("GET /api/disputes - returns all disputes")
    void getDisputes_all() throws Exception {
        Dispute d1 = buildDispute("DSP-A", DisputeStatus.OPEN);
        Dispute d2 = buildDispute("DSP-B", DisputeStatus.UNDER_REVIEW);
        Page<Dispute> page = new PageImpl<>(List.of(d1, d2));

        when(disputeService.getAllDisputes(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/disputes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/disputes?transactionId=TXN-001 - filters by transaction")
    void getDisputes_filterByTransaction() throws Exception {
        Dispute dispute = buildDispute("DSP-C", DisputeStatus.OPEN);
        when(disputeService.getDisputesByTransaction("TXN-001")).thenReturn(List.of(dispute));

        mockMvc.perform(get("/api/disputes")
                        .param("transactionId", "TXN-001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionId").value("TXN-001"));
    }

    // --- PUT /api/disputes/{id}/review ---

    @Test
    @DisplayName("PUT /api/disputes/{id}/review - success returns 200")
    void reviewDispute_success() throws Exception {
        Dispute dispute = buildDispute("DSP-003", DisputeStatus.UNDER_REVIEW);
        when(disputeService.reviewDispute("DSP-003")).thenReturn(dispute);

        mockMvc.perform(put("/api/disputes/DSP-003/review")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("PUT /api/disputes/{id}/review - invalid state returns 400")
    void reviewDispute_invalidState() throws Exception {
        when(disputeService.reviewDispute("DSP-004"))
                .thenThrow(new InvalidTransactionException("Dispute cannot be reviewed in current status: RESOLVED_APPROVED"));

        mockMvc.perform(put("/api/disputes/DSP-004/review")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/disputes/{id}/resolve ---

    @Test
    @DisplayName("PUT /api/disputes/{id}/resolve - success returns 200")
    void resolveDispute_success() throws Exception {
        Dispute dispute = buildDispute("DSP-005", DisputeStatus.RESOLVED_APPROVED);
        dispute.setResolution("Refund approved");
        dispute.setResolvedAt(Instant.now());

        when(disputeService.resolveDispute(eq("DSP-005"), eq("Refund approved"), eq(DisputeStatus.RESOLVED_APPROVED)))
                .thenReturn(dispute);

        Map<String, String> body = Map.of(
                "resolution", "Refund approved",
                "status", "RESOLVED_APPROVED"
        );

        mockMvc.perform(put("/api/disputes/DSP-005/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED_APPROVED"))
                .andExpect(jsonPath("$.resolution").value("Refund approved"));
    }

    @Test
    @DisplayName("PUT /api/disputes/{id}/resolve - not found returns 400")
    void resolveDispute_notFound() throws Exception {
        when(disputeService.resolveDispute(eq("DSP-NONE"), any(), any()))
                .thenThrow(new InvalidTransactionException("Dispute not found: DSP-NONE"));

        Map<String, String> body = Map.of(
                "resolution", "Denied",
                "status", "RESOLVED_DENIED"
        );

        mockMvc.perform(put("/api/disputes/DSP-NONE/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/disputes/{id} ---

    @Test
    @DisplayName("DELETE /api/disputes/{id} - success returns 200")
    void cancelDispute_success() throws Exception {
        Dispute dispute = buildDispute("DSP-006", DisputeStatus.CANCELLED);
        when(disputeService.cancelDispute("DSP-006")).thenReturn(dispute);

        mockMvc.perform(delete("/api/disputes/DSP-006")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("DELETE /api/disputes/{id} - cannot cancel resolved returns 400")
    void cancelDispute_cannotCancelResolved() throws Exception {
        when(disputeService.cancelDispute("DSP-007"))
                .thenThrow(new InvalidTransactionException("Cannot cancel a resolved dispute"));

        mockMvc.perform(delete("/api/disputes/DSP-007")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
