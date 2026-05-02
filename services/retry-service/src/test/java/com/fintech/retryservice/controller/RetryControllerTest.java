package com.fintech.retryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.retryservice.dto.RetryRequest;
import com.fintech.retryservice.dto.RetryResponse;
import com.fintech.retryservice.dto.RetryStatusUpdate;
import com.fintech.retryservice.model.RetryStatus;
import com.fintech.retryservice.model.RetryType;
import com.fintech.retryservice.service.RetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RetryController.class)
@AutoConfigureMockMvc(addFilters = false)
class RetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RetryService retryService;

    private ObjectMapper objectMapper;
    private RetryResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = new RetryResponse();
        sampleResponse.setRetryId("retry-123");
        sampleResponse.setOriginalId("orig-456");
        sampleResponse.setRetryType(RetryType.PAYMENT_PROCESSING);
        sampleResponse.setRetryStatus(RetryStatus.PENDING);
        sampleResponse.setRetryCount(0);
        sampleResponse.setMaxRetries(3);
        sampleResponse.setRetryDelaySeconds(60);
        sampleResponse.setServiceName("payment-service");
        sampleResponse.setCreatedBy("test-user");
        sampleResponse.setPriority("NORMAL");
    }

    @Nested
    @DisplayName("GET /api/retries")
    class GetRetriesTests {

        @Test
        @DisplayName("should return paginated retries without status filter")
        void getRetries_noFilter() throws Exception {
            Page<RetryResponse> page = new PageImpl<>(List.of(sampleResponse));
            when(retryService.getAllRetries(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/retries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].retryId").value("retry-123"))
                    .andExpect(jsonPath("$.content[0].originalId").value("orig-456"))
                    .andExpect(jsonPath("$.content[0].retryStatus").value("PENDING"));
        }

        @Test
        @DisplayName("should return paginated retries with status filter")
        void getRetries_withStatusFilter() throws Exception {
            Page<RetryResponse> page = new PageImpl<>(List.of(sampleResponse));
            when(retryService.getRetriesByStatus(eq(RetryStatus.PENDING), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/retries")
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].retryStatus").value("PENDING"));
        }

        @Test
        @DisplayName("should support pagination parameters")
        void getRetries_pagination() throws Exception {
            Page<RetryResponse> page = new PageImpl<>(List.of(sampleResponse));
            when(retryService.getAllRetries(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/retries")
                            .param("page", "0")
                            .param("size", "5")
                            .param("sort", "createdAt")
                            .param("direction", "ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/retries/{id}")
    class GetRetryByIdTests {

        @Test
        @DisplayName("should return 200 with retry response when found")
        void getRetryById_found() throws Exception {
            when(retryService.getRetryById("retry-123")).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/retries/retry-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.retryId").value("retry-123"))
                    .andExpect(jsonPath("$.serviceName").value("payment-service"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void getRetryById_notFound() throws Exception {
            when(retryService.getRetryById("missing")).thenThrow(new NoSuchElementException("Not found"));

            mockMvc.perform(get("/api/retries/missing"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/retries")
    class CreateRetryTests {

        @Test
        @DisplayName("should return 201 with created retry")
        void createRetry_success() throws Exception {
            RetryRequest request = new RetryRequest();
            request.setOriginalId("orig-789");
            request.setRetryType(RetryType.NOTIFICATION_DELIVERY);
            request.setServiceName("notification-svc");
            request.setCreatedBy("admin");

            RetryResponse createdResponse = new RetryResponse();
            createdResponse.setRetryId("new-id");
            createdResponse.setOriginalId("orig-789");
            createdResponse.setRetryType(RetryType.NOTIFICATION_DELIVERY);
            createdResponse.setRetryStatus(RetryStatus.PENDING);
            createdResponse.setServiceName("notification-svc");
            createdResponse.setCreatedBy("admin");

            when(retryService.scheduleRetry(any(RetryRequest.class))).thenReturn(createdResponse);

            mockMvc.perform(post("/api/retries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.retryId").value("new-id"))
                    .andExpect(jsonPath("$.retryStatus").value("PENDING"));
        }

        @Test
        @DisplayName("should return 400 when required fields are missing")
        void createRetry_validationError() throws Exception {
            RetryRequest request = new RetryRequest();
            // missing required fields

            mockMvc.perform(post("/api/retries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/retries/{id}/retry-now")
    class ForceRetryNowTests {

        @Test
        @DisplayName("should return 200 when force retry succeeds")
        void forceRetryNow_success() throws Exception {
            sampleResponse.setRetryStatus(RetryStatus.PENDING);
            when(retryService.forceRetryNow("retry-123")).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/retries/retry-123/retry-now"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.retryId").value("retry-123"));
        }

        @Test
        @DisplayName("should return 404 when retry not found")
        void forceRetryNow_notFound() throws Exception {
            when(retryService.forceRetryNow("missing"))
                    .thenThrow(new NoSuchElementException("Not found"));

            mockMvc.perform(post("/api/retries/missing/retry-now"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when retry is in terminal state")
        void forceRetryNow_illegalState() throws Exception {
            when(retryService.forceRetryNow("retry-123"))
                    .thenThrow(new IllegalStateException("Cannot force retry"));

            mockMvc.perform(post("/api/retries/retry-123/retry-now"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/retries/{id}")
    class CancelRetryTests {

        @Test
        @DisplayName("should return 200 with cancelled retry")
        void cancelRetry_success() throws Exception {
            sampleResponse.setRetryStatus(RetryStatus.CANCELLED);
            when(retryService.cancelRetry("retry-123")).thenReturn(sampleResponse);

            mockMvc.perform(delete("/api/retries/retry-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.retryStatus").value("CANCELLED"));
        }

        @Test
        @DisplayName("should return 404 when retry not found")
        void cancelRetry_notFound() throws Exception {
            when(retryService.cancelRetry("missing"))
                    .thenThrow(new NoSuchElementException("Not found"));

            mockMvc.perform(delete("/api/retries/missing"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 for completed retry")
        void cancelRetry_illegalState() throws Exception {
            when(retryService.cancelRetry("retry-123"))
                    .thenThrow(new IllegalStateException("Cannot cancel completed"));

            mockMvc.perform(delete("/api/retries/retry-123"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/retries/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("should return 200 with updated retry")
        void updateStatus_success() throws Exception {
            RetryStatusUpdate update = new RetryStatusUpdate();
            update.setRetryStatus(RetryStatus.COMPLETED);
            update.setUpdatedBy("callback-svc");

            sampleResponse.setRetryStatus(RetryStatus.COMPLETED);
            when(retryService.updateStatus(eq("retry-123"), any(RetryStatusUpdate.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(put("/api/retries/retry-123/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.retryStatus").value("COMPLETED"));
        }

        @Test
        @DisplayName("should return 404 when retry not found")
        void updateStatus_notFound() throws Exception {
            RetryStatusUpdate update = new RetryStatusUpdate();
            update.setRetryStatus(RetryStatus.FAILED);
            update.setUpdatedBy("system");

            when(retryService.updateStatus(eq("missing"), any(RetryStatusUpdate.class)))
                    .thenThrow(new NoSuchElementException("Not found"));

            mockMvc.perform(put("/api/retries/missing/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when required fields missing")
        void updateStatus_validationError() throws Exception {
            RetryStatusUpdate update = new RetryStatusUpdate();
            // missing retryStatus and updatedBy

            mockMvc.perform(put("/api/retries/retry-123/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/retries/statistics")
    class GetStatisticsTests {

        @Test
        @DisplayName("should return 200 with statistics map")
        void getStatistics_success() throws Exception {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("pending", 10L);
            stats.put("inProgress", 2L);
            stats.put("completed", 50L);
            stats.put("failed", 3L);
            stats.put("cancelled", 1L);
            stats.put("maxRetriesExceeded", 4L);
            stats.put("breakdown", List.of());
            stats.put("timestamp", LocalDateTime.now().toString());

            when(retryService.getStatistics()).thenReturn(stats);

            mockMvc.perform(get("/api/retries/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pending").value(10))
                    .andExpect(jsonPath("$.completed").value(50))
                    .andExpect(jsonPath("$.cancelled").value(1));
        }
    }
}
