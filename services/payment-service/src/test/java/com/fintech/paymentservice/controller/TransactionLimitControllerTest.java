package com.fintech.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.dto.request.LimitCheckRequest;
import com.fintech.paymentservice.dto.request.TransactionLimitRequest;
import com.fintech.paymentservice.dto.response.TransactionLimitResponse;
import com.fintech.paymentservice.entity.LimitType;
import com.fintech.paymentservice.exception.TransactionLimitExceededException;
import com.fintech.paymentservice.service.TransactionLimitService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionLimitController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionLimitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionLimitService limitService;

    private TransactionLimitResponse sampleLimitResponse() {
        return new TransactionLimitResponse(1L, "ACC001", LimitType.DAILY,
                new BigDecimal("10000.00"), new BigDecimal("2000.00"),
                new BigDecimal("8000.00"), Instant.now().plusSeconds(86400), "USD", true);
    }

    // --- POST /api/accounts/{accountId}/limits ---

    @Test
    @DisplayName("POST /api/accounts/{accountId}/limits returns 201 on success")
    void createLimit_success() throws Exception {
        when(limitService.updateLimit(eq("ACC001"), eq(LimitType.DAILY), any(BigDecimal.class), any()))
                .thenReturn(sampleLimitResponse());

        TransactionLimitRequest request = new TransactionLimitRequest(
                LimitType.DAILY, new BigDecimal("10000.00"), true);

        mockMvc.perform(post("/api/accounts/ACC001/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value("ACC001"))
                .andExpect(jsonPath("$.limitType").value("DAILY"));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountId}/limits returns 400 for invalid request")
    void createLimit_validationError() throws Exception {
        // Missing required limitType
        String invalidJson = "{\"maxAmount\":\"10000.00\"}";

        mockMvc.perform(post("/api/accounts/ACC001/limits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/accounts/{accountId}/limits ---

    @Test
    @DisplayName("GET /api/accounts/{accountId}/limits returns 200 with limits list")
    void getLimits_success() throws Exception {
        when(limitService.getLimits("ACC001")).thenReturn(List.of(sampleLimitResponse()));

        mockMvc.perform(get("/api/accounts/ACC001/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].accountId").value("ACC001"));
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId}/limits returns 200 with empty list")
    void getLimits_empty() throws Exception {
        when(limitService.getLimits("ACC001")).thenReturn(List.of());

        mockMvc.perform(get("/api/accounts/ACC001/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- PUT /api/accounts/{accountId}/limits/{type} ---

    @Test
    @DisplayName("PUT /api/accounts/{accountId}/limits/{type} returns 200 on success")
    void updateLimit_success() throws Exception {
        when(limitService.updateLimit(eq("ACC001"), eq(LimitType.DAILY), any(BigDecimal.class), any()))
                .thenReturn(sampleLimitResponse());

        TransactionLimitRequest request = new TransactionLimitRequest(
                LimitType.DAILY, new BigDecimal("20000.00"), true);

        mockMvc.perform(put("/api/accounts/ACC001/limits/DAILY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC001"));
    }

    @Test
    @DisplayName("PUT /api/accounts/{accountId}/limits/{type} returns 400 for invalid limit type")
    void updateLimit_invalidType() throws Exception {
        TransactionLimitRequest request = new TransactionLimitRequest(
                LimitType.DAILY, new BigDecimal("20000.00"), true);

        mockMvc.perform(put("/api/accounts/ACC001/limits/INVALID_TYPE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid limit type"));
    }

    // --- POST /api/accounts/{accountId}/limits/check ---

    @Test
    @DisplayName("POST /api/accounts/{accountId}/limits/check returns 200 when within limits")
    void checkLimits_allowed() throws Exception {
        doNothing().when(limitService).checkLimits(eq("ACC001"), any(BigDecimal.class));

        LimitCheckRequest request = new LimitCheckRequest(new BigDecimal("500.00"), "USD");

        mockMvc.perform(post("/api/accounts/ACC001/limits/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.accountId").value("ACC001"));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountId}/limits/check returns 429 when limit exceeded")
    void checkLimits_exceeded() throws Exception {
        doThrow(new TransactionLimitExceededException("DAILY", "ACC001", "Daily limit exceeded"))
                .when(limitService).checkLimits(eq("ACC001"), any(BigDecimal.class));

        LimitCheckRequest request = new LimitCheckRequest(new BigDecimal("99999.00"), "USD");

        mockMvc.perform(post("/api/accounts/ACC001/limits/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountId}/limits/check returns 400 for invalid request")
    void checkLimits_validationError() throws Exception {
        // Missing required amount
        String invalidJson = "{\"currency\":\"USD\"}";

        mockMvc.perform(post("/api/accounts/ACC001/limits/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
