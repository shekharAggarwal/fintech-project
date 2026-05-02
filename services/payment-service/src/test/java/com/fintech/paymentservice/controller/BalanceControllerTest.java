package com.fintech.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.dto.request.BalanceOperationRequest;
import com.fintech.paymentservice.dto.response.BalanceResponse;
import com.fintech.paymentservice.exception.InsufficientFundsException;
import com.fintech.paymentservice.service.BalanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BalanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BalanceService balanceService;

    private BalanceResponse sampleResponse() {
        return new BalanceResponse(1L, "ACC001", new BigDecimal("1000.00"),
                new BigDecimal("900.00"), new BigDecimal("100.00"), "USD", Instant.now());
    }

    // --- GET /api/accounts/{accountNumber}/balance ---

    @Test
    @DisplayName("GET /api/accounts/{accountNumber}/balance returns 200 with balance")
    void getBalance_success() throws Exception {
        when(balanceService.getBalance("ACC001")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/accounts/ACC001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC001"))
                .andExpect(jsonPath("$.currentBalance").value(1000.00));
    }

    @Test
    @DisplayName("GET /api/accounts/{accountNumber}/balance returns 404 when account not found")
    void getBalance_notFound() throws Exception {
        when(balanceService.getBalance("INVALID")).thenThrow(new RuntimeException("Account not found"));

        mockMvc.perform(get("/api/accounts/INVALID/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account not found"));
    }

    // --- POST /api/accounts/{accountNumber}/credit ---

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/credit returns 200 on success")
    void credit_success() throws Exception {
        when(balanceService.credit(eq("ACC001"), any(BigDecimal.class), any()))
                .thenReturn(sampleResponse());

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("500.00"), "USD", "Salary credit", "REF001");

        mockMvc.perform(post("/api/accounts/ACC001/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC001"));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/credit returns 400 on failure")
    void credit_failure() throws Exception {
        when(balanceService.credit(eq("ACC001"), any(BigDecimal.class), any()))
                .thenThrow(new RuntimeException("Credit processing error"));

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("500.00"), "USD", "test", "REF001");

        mockMvc.perform(post("/api/accounts/ACC001/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Credit failed"));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/credit returns 400 for invalid request body")
    void credit_validationError() throws Exception {
        // Missing required amount field
        String invalidJson = "{\"accountNumber\":\"ACC001\"}";

        mockMvc.perform(post("/api/accounts/ACC001/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/accounts/{accountNumber}/debit ---

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/debit returns 200 on success")
    void debit_success() throws Exception {
        when(balanceService.debit(eq("ACC001"), any(BigDecimal.class), any()))
                .thenReturn(sampleResponse());

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("200.00"), "USD", "Purchase", "REF002");

        mockMvc.perform(post("/api/accounts/ACC001/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC001"));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/debit returns 409 on insufficient funds")
    void debit_insufficientFunds() throws Exception {
        when(balanceService.debit(eq("ACC001"), any(BigDecimal.class), any()))
                .thenThrow(new InsufficientFundsException("ACC001", "Insufficient funds"));

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("99999.00"), "USD", "Too much", "REF003");

        mockMvc.perform(post("/api/accounts/ACC001/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Insufficient funds"));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/debit returns 400 on other errors")
    void debit_otherError() throws Exception {
        when(balanceService.debit(eq("ACC001"), any(BigDecimal.class), any()))
                .thenThrow(new RuntimeException("Debit processing error"));

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("200.00"), "USD", "test", "REF004");

        mockMvc.perform(post("/api/accounts/ACC001/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debit failed"));
    }

    // --- POST /api/accounts/{accountNumber}/hold ---

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/hold returns 200 on success")
    void placeHold_success() throws Exception {
        when(balanceService.placeHold(eq("ACC001"), any(BigDecimal.class), any()))
                .thenReturn(sampleResponse());

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("300.00"), "USD", "Hotel hold", "REF005");

        mockMvc.perform(post("/api/accounts/ACC001/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC001"));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/hold returns 409 on insufficient funds")
    void placeHold_insufficientFunds() throws Exception {
        when(balanceService.placeHold(eq("ACC001"), any(BigDecimal.class), any()))
                .thenThrow(new InsufficientFundsException("ACC001", "Insufficient funds for hold"));

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("99999.00"), "USD", "Big hold", "REF006");

        mockMvc.perform(post("/api/accounts/ACC001/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Insufficient funds for hold"));
    }

    // --- POST /api/accounts/{accountNumber}/release-hold ---

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/release-hold returns 200 on success")
    void releaseHold_success() throws Exception {
        when(balanceService.releaseHold(eq("ACC001"), any(BigDecimal.class), any()))
                .thenReturn(sampleResponse());

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("100.00"), "USD", "Release hold", "REF007");

        mockMvc.perform(post("/api/accounts/ACC001/release-hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC001"));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountNumber}/release-hold returns 400 on failure")
    void releaseHold_failure() throws Exception {
        when(balanceService.releaseHold(eq("ACC001"), any(BigDecimal.class), any()))
                .thenThrow(new RuntimeException("Release hold error"));

        BalanceOperationRequest request = new BalanceOperationRequest("ACC001",
                new BigDecimal("100.00"), "USD", "test", "REF008");

        mockMvc.perform(post("/api/accounts/ACC001/release-hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Release hold failed"));
    }
}
