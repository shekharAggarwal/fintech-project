package com.fintech.schedulerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.schedulerservice.dto.RecurringPaymentRequest;
import com.fintech.schedulerservice.dto.RecurringPaymentResponse;
import com.fintech.schedulerservice.dto.RecurringPaymentUpdateRequest;
import com.fintech.schedulerservice.entity.PaymentFrequency;
import com.fintech.schedulerservice.entity.RecurringPaymentStatus;
import com.fintech.schedulerservice.exception.InvalidJobStateException;
import com.fintech.schedulerservice.exception.JobNotFoundException;
import com.fintech.schedulerservice.service.RecurringPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecurringPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecurringPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecurringPaymentService recurringPaymentService;

    private ObjectMapper objectMapper;
    private RecurringPaymentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = new RecurringPaymentResponse();
        sampleResponse.setId("pay-001");
        sampleResponse.setUserId("user-123");
        sampleResponse.setSourceAccountId("acc-src-001");
        sampleResponse.setDestinationAccountId("acc-dst-001");
        sampleResponse.setAmount(new BigDecimal("100.00"));
        sampleResponse.setCurrency("USD");
        sampleResponse.setFrequency(PaymentFrequency.MONTHLY);
        sampleResponse.setStatus(RecurringPaymentStatus.ACTIVE);
        sampleResponse.setStartDate(LocalDate.now());
        sampleResponse.setEndDate(LocalDate.now().plusYears(1));
        sampleResponse.setNextExecutionDate(LocalDate.now());
        sampleResponse.setMaxRetries(3);
        sampleResponse.setCurrentRetryCount(0);
        sampleResponse.setDescription("Monthly rent");
        sampleResponse.setCreatedAt(Instant.now());
        sampleResponse.setUpdatedAt(Instant.now());
    }

    @Test
    void createRecurringPayment_shouldReturn201() throws Exception {
        RecurringPaymentRequest request = new RecurringPaymentRequest(
                "user-123", "acc-src-001", "acc-dst-001",
                new BigDecimal("100.00"), "USD", PaymentFrequency.MONTHLY,
                LocalDate.now(), LocalDate.now().plusYears(1), "Monthly rent", 3
        );

        when(recurringPaymentService.createRecurringPayment(any(RecurringPaymentRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/recurring-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pay-001"))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getRecurringPayments_withUserId_shouldReturn200() throws Exception {
        Page<RecurringPaymentResponse> page = new PageImpl<>(
                List.of(sampleResponse), PageRequest.of(0, 20), 1);
        when(recurringPaymentService.getPaymentsByUserId(eq("user-123"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/recurring-payments")
                        .param("userId", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("pay-001"))
                .andExpect(jsonPath("$.content[0].userId").value("user-123"));
    }

    @Test
    void getRecurringPayments_withoutUserId_shouldReturn200() throws Exception {
        Page<RecurringPaymentResponse> page = new PageImpl<>(
                List.of(sampleResponse), PageRequest.of(0, 20), 1);
        when(recurringPaymentService.getAllPayments(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/recurring-payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("pay-001"));
    }

    @Test
    void getRecurringPaymentById_shouldReturn200() throws Exception {
        when(recurringPaymentService.getPaymentById("pay-001")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/recurring-payments/pay-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay-001"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void getRecurringPaymentById_shouldReturn404WhenNotFound() throws Exception {
        when(recurringPaymentService.getPaymentById("not-found"))
                .thenThrow(new JobNotFoundException("not-found"));

        mockMvc.perform(get("/api/recurring-payments/not-found"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRecurringPayment_shouldReturn200() throws Exception {
        RecurringPaymentUpdateRequest updateRequest = new RecurringPaymentUpdateRequest(
                new BigDecimal("200.00"), "EUR", null, null, null, "Updated", 5
        );

        when(recurringPaymentService.updatePayment(eq("pay-001"), any(RecurringPaymentUpdateRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/recurring-payments/pay-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay-001"));
    }

    @Test
    void updateRecurringPayment_shouldReturn404WhenNotFound() throws Exception {
        RecurringPaymentUpdateRequest updateRequest = new RecurringPaymentUpdateRequest(
                new BigDecimal("200.00"), null, null, null, null, null, null
        );

        when(recurringPaymentService.updatePayment(eq("not-found"), any(RecurringPaymentUpdateRequest.class)))
                .thenThrow(new JobNotFoundException("not-found"));

        mockMvc.perform(put("/api/recurring-payments/not-found")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void pauseRecurringPayment_shouldReturn200() throws Exception {
        sampleResponse.setStatus(RecurringPaymentStatus.PAUSED);
        when(recurringPaymentService.pausePayment("pay-001")).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/recurring-payments/pay-001/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay-001"))
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void pauseRecurringPayment_shouldReturn409WhenInvalidState() throws Exception {
        when(recurringPaymentService.pausePayment("pay-001"))
                .thenThrow(new InvalidJobStateException("pay-001", null, "pause"));

        mockMvc.perform(post("/api/recurring-payments/pay-001/pause"))
                .andExpect(status().isConflict());
    }

    @Test
    void resumeRecurringPayment_shouldReturn200() throws Exception {
        when(recurringPaymentService.resumePayment("pay-001")).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/recurring-payments/pay-001/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay-001"));
    }

    @Test
    void resumeRecurringPayment_shouldReturn409WhenInvalidState() throws Exception {
        when(recurringPaymentService.resumePayment("pay-001"))
                .thenThrow(new InvalidJobStateException("pay-001", null, "resume"));

        mockMvc.perform(post("/api/recurring-payments/pay-001/resume"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelRecurringPayment_shouldReturn204() throws Exception {
        doNothing().when(recurringPaymentService).cancelPayment("pay-001");

        mockMvc.perform(delete("/api/recurring-payments/pay-001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelRecurringPayment_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new JobNotFoundException("not-found"))
                .when(recurringPaymentService).cancelPayment("not-found");

        mockMvc.perform(delete("/api/recurring-payments/not-found"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelRecurringPayment_shouldReturn409WhenInvalidState() throws Exception {
        doThrow(new InvalidJobStateException("pay-001", null, "cancel"))
                .when(recurringPaymentService).cancelPayment("pay-001");

        mockMvc.perform(delete("/api/recurring-payments/pay-001"))
                .andExpect(status().isConflict());
    }
}
