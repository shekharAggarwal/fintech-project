package com.fintech.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.dto.request.DepositRequest;
import com.fintech.paymentservice.dto.request.InitiateRequest;
import com.fintech.paymentservice.dto.request.BulkTransferRequest;
import com.fintech.paymentservice.dto.request.WithdrawRequest;
import com.fintech.paymentservice.dto.response.BulkTransferResponse;
import com.fintech.paymentservice.dto.response.PaymentInitiatedResponse;
import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.exception.InsufficientFundsException;
import com.fintech.paymentservice.exception.TransactionLimitExceededException;
import com.fintech.paymentservice.fraud.exception.PaymentBlockedByFraudException;
import com.fintech.paymentservice.model.PaymentStatus;
import com.fintech.paymentservice.service.PaymentService;
import com.fintech.security.service.AuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AuthorizationService authorizationService;

    private static final String USER_ID = "user-123";

    private PaymentInitiatedResponse samplePaymentResponse() {
        return new PaymentInitiatedResponse("PAY001", "ACC001", "ACC002",
                new BigDecimal("100.00"), "Test payment", PaymentStatus.PENDING_VERIFICATION,
                Instant.now(), "Payment initiated successfully.");
    }

    private Payment samplePayment() {
        Payment payment = new Payment();
        payment.setPaymentId("PAY001");
        payment.setUserId(USER_ID);
        payment.setFromAccount("ACC001");
        payment.setToAccount("ACC002");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setStatus(PaymentStatus.PENDING_VERIFICATION);
        payment.setCreatedAt(Instant.now());
        return payment;
    }

    // --- POST /api/payments/transfer ---

    @Test
    @DisplayName("POST /api/payments/transfer returns 200 on success")
    void initiate_success() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.initiate(any(InitiateRequest.class), eq(USER_ID)))
                .thenReturn(samplePaymentResponse());

        InitiateRequest request = new InitiateRequest("ACC001", "ACC002",
                new BigDecimal("100.00"), "Test payment");

        mockMvc.perform(post("/api/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY001"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
    }

    @Test
    @DisplayName("POST /api/payments/transfer returns 400 when no user context")
    void initiate_noUserContext() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(null);

        InitiateRequest request = new InitiateRequest("ACC001", "ACC002",
                new BigDecimal("100.00"), "Test payment");

        mockMvc.perform(post("/api/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No user context"));
    }

    @Test
    @DisplayName("POST /api/payments/transfer returns 409 on insufficient funds")
    void initiate_insufficientFunds() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.initiate(any(InitiateRequest.class), eq(USER_ID)))
                .thenThrow(new InsufficientFundsException("ACC001", "Insufficient funds"));

        InitiateRequest request = new InitiateRequest("ACC001", "ACC002",
                new BigDecimal("99999.00"), "Big payment");

        mockMvc.perform(post("/api/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Insufficient funds"));
    }

    @Test
    @DisplayName("POST /api/payments/transfer returns 429 on transaction limit exceeded")
    void initiate_limitExceeded() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.initiate(any(InitiateRequest.class), eq(USER_ID)))
                .thenThrow(new TransactionLimitExceededException("DAILY", "ACC001", "Daily limit exceeded"));

        InitiateRequest request = new InitiateRequest("ACC001", "ACC002",
                new BigDecimal("100.00"), "Test");

        mockMvc.perform(post("/api/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Transaction limit exceeded"));
    }

    @Test
    @DisplayName("POST /api/payments/transfer returns 403 when blocked by fraud")
    void initiate_blockedByFraud() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.initiate(any(InitiateRequest.class), eq(USER_ID)))
                .thenThrow(new PaymentBlockedByFraudException("PAY001", "High risk", 80));

        InitiateRequest request = new InitiateRequest("ACC001", "ACC002",
                new BigDecimal("100.00"), "Test");

        mockMvc.perform(post("/api/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Payment blocked"));
    }

    @Test
    @DisplayName("POST /api/payments/transfer returns 500 on unexpected error")
    void initiate_unexpectedError() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.initiate(any(InitiateRequest.class), eq(USER_ID)))
                .thenThrow(new RuntimeException("Unexpected error"));

        InitiateRequest request = new InitiateRequest("ACC001", "ACC002",
                new BigDecimal("100.00"), "Test");

        mockMvc.perform(post("/api/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Payment initiation failed"));
    }

    @Test
    @DisplayName("POST /api/payments/transfer returns 400 for invalid request body")
    void initiate_validationError() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);

        // Missing required fields
        String invalidJson = "{\"fromAccount\":\"\",\"amount\":null}";

        mockMvc.perform(post("/api/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/payments/{paymentId} ---

    @Test
    @DisplayName("GET /api/payments/{paymentId} returns 200 with payment")
    void getPaymentStatus_success() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.getPaymentStatus("PAY001", USER_ID))
                .thenReturn(Optional.of(samplePayment()));

        mockMvc.perform(get("/api/payments/PAY001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY001"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} returns 404 when not found")
    void getPaymentStatus_notFound() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.getPaymentStatus("PAY999", USER_ID))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/PAY999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} returns 400 when no user context")
    void getPaymentStatus_noUserContext() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(null);

        mockMvc.perform(get("/api/payments/PAY001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No user context"));
    }

    // --- POST /api/payments/deposit ---

    @Test
    @DisplayName("POST /api/payments/deposit returns 200 on success")
    void deposit_success() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.deposit(eq("ACC001"), any(BigDecimal.class), any(), eq(USER_ID)))
                .thenReturn(samplePaymentResponse());

        DepositRequest request = new DepositRequest("ACC001", new BigDecimal("500.00"), "Deposit");

        mockMvc.perform(post("/api/payments/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY001"));
    }

    @Test
    @DisplayName("POST /api/payments/deposit returns 500 on error")
    void deposit_error() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.deposit(eq("ACC001"), any(BigDecimal.class), any(), eq(USER_ID)))
                .thenThrow(new RuntimeException("Deposit failed"));

        DepositRequest request = new DepositRequest("ACC001", new BigDecimal("500.00"), "Deposit");

        mockMvc.perform(post("/api/payments/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Deposit initiation failed"));
    }

    // --- POST /api/payments/withdraw ---

    @Test
    @DisplayName("POST /api/payments/withdraw returns 200 on success")
    void withdraw_success() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.withdraw(eq("ACC001"), any(BigDecimal.class), any(), eq(USER_ID)))
                .thenReturn(samplePaymentResponse());

        WithdrawRequest request = new WithdrawRequest("ACC001", new BigDecimal("200.00"), "Withdrawal");

        mockMvc.perform(post("/api/payments/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY001"));
    }

    @Test
    @DisplayName("POST /api/payments/withdraw returns 500 on error")
    void withdraw_error() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.withdraw(eq("ACC001"), any(BigDecimal.class), any(), eq(USER_ID)))
                .thenThrow(new RuntimeException("Withdrawal failed"));

        WithdrawRequest request = new WithdrawRequest("ACC001", new BigDecimal("200.00"), "Withdrawal");

        mockMvc.perform(post("/api/payments/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Withdrawal initiation failed"));
    }

    // --- GET /api/payments/history ---

    @Test
    @DisplayName("GET /api/payments/history returns 200 with payment history")
    void getPaymentHistory_success() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        Page<Payment> page = new PageImpl<>(List.of(samplePayment()), PageRequest.of(0, 20), 1);
        when(paymentService.getPaymentHistory(USER_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/payments/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments").isArray())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("GET /api/payments/history returns 400 when no user context")
    void getPaymentHistory_noUserContext() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(null);

        mockMvc.perform(get("/api/payments/history"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No user context"));
    }

    // --- POST /api/payments/{paymentId}/cancel ---

    @Test
    @DisplayName("POST /api/payments/{paymentId}/cancel returns 200 on success")
    void cancelPayment_success() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.cancelPayment("PAY001", USER_ID)).thenReturn(true);

        mockMvc.perform(post("/api/payments/PAY001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment cancelled successfully"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/cancel returns 400 when not cancellable")
    void cancelPayment_notCancellable() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.cancelPayment("PAY001", USER_ID)).thenReturn(false);

        mockMvc.perform(post("/api/payments/PAY001/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot cancel payment"));
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/cancel returns 500 on error")
    void cancelPayment_error() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.cancelPayment("PAY001", USER_ID))
                .thenThrow(new RuntimeException("Cancel error"));

        mockMvc.perform(post("/api/payments/PAY001/cancel"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Payment cancellation failed"));
    }

    // --- POST /api/payments/bulk-transfer ---

    @Test
    @DisplayName("POST /api/payments/bulk-transfer returns 200 on success")
    void bulkTransfer_success() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        BulkTransferResponse response = new BulkTransferResponse(
                List.of(samplePaymentResponse()), List.of(), 1, 1, 0);
        when(paymentService.processBulkTransfers(anyList(), eq(USER_ID))).thenReturn(response);

        BulkTransferRequest request = new BulkTransferRequest(
                List.of(new InitiateRequest("ACC001", "ACC002", new BigDecimal("100.00"), "Test")));

        mockMvc.perform(post("/api/payments/bulk-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful").value(1))
                .andExpect(jsonPath("$.failed").value(0));
    }

    @Test
    @DisplayName("POST /api/payments/bulk-transfer returns 400 when no user context")
    void bulkTransfer_noUserContext() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(null);

        BulkTransferRequest request = new BulkTransferRequest(
                List.of(new InitiateRequest("ACC001", "ACC002", new BigDecimal("100.00"), "Test")));

        mockMvc.perform(post("/api/payments/bulk-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No user context"));
    }

    @Test
    @DisplayName("POST /api/payments/bulk-transfer returns 500 on error")
    void bulkTransfer_error() throws Exception {
        when(authorizationService.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentService.processBulkTransfers(anyList(), eq(USER_ID)))
                .thenThrow(new RuntimeException("Bulk transfer error"));

        BulkTransferRequest request = new BulkTransferRequest(
                List.of(new InitiateRequest("ACC001", "ACC002", new BigDecimal("100.00"), "Test")));

        mockMvc.perform(post("/api/payments/bulk-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Bulk transfer failed"));
    }
}
