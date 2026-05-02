package com.fintech.paymentservice.fraud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.fraud.entity.FraudAlert;
import com.fintech.paymentservice.fraud.model.AlertStatus;
import com.fintech.paymentservice.fraud.model.AlertType;
import com.fintech.paymentservice.fraud.repository.FraudAlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FraudController.class)
@AutoConfigureMockMvc(addFilters = false)
class FraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FraudAlertRepository alertRepository;

    private FraudAlert sampleAlert() {
        FraudAlert alert = new FraudAlert("PAY001", "ACC001", "user-123",
                AlertType.VELOCITY_BREACH, 75, new BigDecimal("5000.00"), "High velocity detected");
        alert.setId(1L);
        alert.setCreatedAt(Instant.now());
        alert.setUpdatedAt(Instant.now());
        return alert;
    }

    // --- GET /api/admin/fraud/alerts ---

    @Test
    @DisplayName("GET /api/admin/fraud/alerts returns 200 with paginated alerts")
    void listAlerts_success() throws Exception {
        Page<FraudAlert> page = new PageImpl<>(List.of(sampleAlert()), PageRequest.of(0, 20), 1);
        when(alertRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/fraud/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].paymentId").value("PAY001"));
    }

    @Test
    @DisplayName("GET /api/admin/fraud/alerts with status filter returns filtered alerts")
    void listAlerts_withStatusFilter() throws Exception {
        Page<FraudAlert> page = new PageImpl<>(List.of(sampleAlert()), PageRequest.of(0, 20), 1);
        when(alertRepository.findByStatus(eq(AlertStatus.OPEN), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/fraud/alerts").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/admin/fraud/alerts with pagination params")
    void listAlerts_withPagination() throws Exception {
        Page<FraudAlert> page = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);
        when(alertRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/fraud/alerts")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // --- GET /api/admin/fraud/alerts/{alertId} ---

    @Test
    @DisplayName("GET /api/admin/fraud/alerts/{alertId} returns 200 when found")
    void getAlert_success() throws Exception {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(sampleAlert()));

        mockMvc.perform(get("/api/admin/fraud/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY001"))
                .andExpect(jsonPath("$.accountId").value("ACC001"));
    }

    @Test
    @DisplayName("GET /api/admin/fraud/alerts/{alertId} returns 404 when not found")
    void getAlert_notFound() throws Exception {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/fraud/alerts/999"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/admin/fraud/alerts/payment/{paymentId} ---

    @Test
    @DisplayName("GET /api/admin/fraud/alerts/payment/{paymentId} returns 200")
    void getAlertsByPayment_success() throws Exception {
        when(alertRepository.findByPaymentId("PAY001")).thenReturn(List.of(sampleAlert()));

        mockMvc.perform(get("/api/admin/fraud/alerts/payment/PAY001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].paymentId").value("PAY001"));
    }

    @Test
    @DisplayName("GET /api/admin/fraud/alerts/payment/{paymentId} returns empty for unknown payment")
    void getAlertsByPayment_empty() throws Exception {
        when(alertRepository.findByPaymentId("PAY999")).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/fraud/alerts/payment/PAY999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/admin/fraud/alerts/account/{accountId} ---

    @Test
    @DisplayName("GET /api/admin/fraud/alerts/account/{accountId} returns 200")
    void getAlertsByAccount_success() throws Exception {
        when(alertRepository.findByAccountId("ACC001")).thenReturn(List.of(sampleAlert()));

        mockMvc.perform(get("/api/admin/fraud/alerts/account/ACC001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].accountId").value("ACC001"));
    }

    // --- PUT /api/admin/fraud/alerts/{alertId}/status ---

    @Test
    @DisplayName("PUT /api/admin/fraud/alerts/{alertId}/status returns 200 on success")
    void updateAlertStatus_success() throws Exception {
        FraudAlert alert = sampleAlert();
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(FraudAlert.class))).thenReturn(alert);

        Map<String, String> body = Map.of(
                "status", "CONFIRMED_FRAUD",
                "resolvedBy", "admin-user",
                "notes", "Confirmed suspicious activity"
        );

        mockMvc.perform(put("/api/admin/fraud/alerts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY001"));
    }

    @Test
    @DisplayName("PUT /api/admin/fraud/alerts/{alertId}/status returns 404 when alert not found")
    void updateAlertStatus_notFound() throws Exception {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        Map<String, String> body = Map.of("status", "RESOLVED");

        mockMvc.perform(put("/api/admin/fraud/alerts/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/admin/fraud/alerts/{alertId}/status returns 400 for invalid status")
    void updateAlertStatus_invalidStatus() throws Exception {
        FraudAlert alert = sampleAlert();
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        Map<String, String> body = Map.of("status", "INVALID_STATUS");

        mockMvc.perform(put("/api/admin/fraud/alerts/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid status"));
    }

    // --- GET /api/admin/fraud/stats ---

    @Test
    @DisplayName("GET /api/admin/fraud/stats returns 200 with statistics")
    void getStats_success() throws Exception {
        when(alertRepository.count()).thenReturn(100L);
        Page<FraudAlert> openPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 25);
        Page<FraudAlert> confirmedPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 10);
        Page<FraudAlert> fpPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 5);

        when(alertRepository.findByStatus(eq(AlertStatus.OPEN), any(Pageable.class))).thenReturn(openPage);
        when(alertRepository.findByStatus(eq(AlertStatus.CONFIRMED_FRAUD), any(Pageable.class))).thenReturn(confirmedPage);
        when(alertRepository.findByStatus(eq(AlertStatus.FALSE_POSITIVE), any(Pageable.class))).thenReturn(fpPage);

        mockMvc.perform(get("/api/admin/fraud/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlerts").value(100))
                .andExpect(jsonPath("$.openAlerts").value(25))
                .andExpect(jsonPath("$.confirmedFraud").value(10))
                .andExpect(jsonPath("$.falsePositives").value(5));
    }
}
