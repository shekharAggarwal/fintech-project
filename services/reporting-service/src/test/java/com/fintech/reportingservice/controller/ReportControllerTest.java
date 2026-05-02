package com.fintech.reportingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.reportingservice.dto.request.GenerateReportRequest;
import com.fintech.reportingservice.dto.response.ReportDownloadResponse;
import com.fintech.reportingservice.dto.response.ReportResponse;
import com.fintech.reportingservice.exception.GlobalExceptionHandler;
import com.fintech.reportingservice.exception.ReportNotFoundException;
import com.fintech.reportingservice.exception.ReportServiceException;
import com.fintech.reportingservice.model.ReportFormat;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.model.ReportType;
import com.fintech.reportingservice.service.ReportService;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    private ObjectMapper objectMapper;
    private ReportResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = new ReportResponse();
        sampleResponse.setReportId("report-123");
        sampleResponse.setReportName("Monthly Summary");
        sampleResponse.setReportType(ReportType.MONTHLY_SUMMARY);
        sampleResponse.setReportStatus(ReportStatus.PENDING);
        sampleResponse.setReportFormat(ReportFormat.PDF);
        sampleResponse.setCreatedBy("user1");
        sampleResponse.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        sampleResponse.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        sampleResponse.setReadyForDownload(false);
    }

    @Nested
    @DisplayName("POST /api/reports")
    class GenerateReportEndpoint {

        @Test
        @DisplayName("should return 202 Accepted with report response")
        void generateReport_success() throws Exception {
            when(reportService.generateReport(any(GenerateReportRequest.class), eq("user1")))
                    .thenReturn(sampleResponse);

            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportName("Monthly Summary");
            request.setReportType(ReportType.MONTHLY_SUMMARY);
            request.setReportFormat(ReportFormat.PDF);

            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "user1")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.reportId").value("report-123"))
                    .andExpect(jsonPath("$.reportName").value("Monthly Summary"))
                    .andExpect(jsonPath("$.reportType").value("MONTHLY_SUMMARY"))
                    .andExpect(jsonPath("$.reportStatus").value("PENDING"))
                    .andExpect(jsonPath("$.createdBy").value("user1"));
        }

        @Test
        @DisplayName("should use 'system' as default userId when header is absent")
        void generateReport_defaultUserId() throws Exception {
            when(reportService.generateReport(any(GenerateReportRequest.class), eq("system")))
                    .thenReturn(sampleResponse);

            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportName("Test Report");
            request.setReportType(ReportType.TRANSACTION_SUMMARY);
            request.setReportFormat(ReportFormat.CSV);

            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());

            verify(reportService).generateReport(any(GenerateReportRequest.class), eq("system"));
        }

        @Test
        @DisplayName("should return 400 when reportName is blank")
        void generateReport_missingName() throws Exception {
            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportType(ReportType.MONTHLY_SUMMARY);
            request.setReportFormat(ReportFormat.PDF);

            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "user1")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when reportType is null")
        void generateReport_missingType() throws Exception {
            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportName("Some Report");
            request.setReportFormat(ReportFormat.PDF);

            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "user1")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when reportFormat is null")
        void generateReport_missingFormat() throws Exception {
            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportName("Some Report");
            request.setReportType(ReportType.MONTHLY_SUMMARY);

            mockMvc.perform(post("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", "user1")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/reports/{id}")
    class GetReportStatusEndpoint {

        @Test
        @DisplayName("should return 200 with report details")
        void getReportStatus_success() throws Exception {
            when(reportService.getReportStatus("report-123")).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/reports/report-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reportId").value("report-123"))
                    .andExpect(jsonPath("$.reportName").value("Monthly Summary"))
                    .andExpect(jsonPath("$.reportStatus").value("PENDING"));
        }

        @Test
        @DisplayName("should return 404 when report not found")
        void getReportStatus_notFound() throws Exception {
            when(reportService.getReportStatus("nonexistent"))
                    .thenThrow(new ReportNotFoundException("nonexistent"));

            mockMvc.perform(get("/api/reports/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/reports/{id}/download")
    class DownloadReportEndpoint {

        @Test
        @DisplayName("should return 200 with download response")
        void downloadReport_success() throws Exception {
            ReportDownloadResponse downloadResponse = new ReportDownloadResponse();
            downloadResponse.setReportId("report-123");
            downloadResponse.setReportName("Monthly Summary");
            downloadResponse.setFileName("Monthly Summary.pdf");
            downloadResponse.setContentType("application/pdf");
            downloadResponse.setFileSizeBytes(1024L);
            downloadResponse.setDownloadUrl("/reports/monthly-summary.pdf");

            when(reportService.downloadReport("report-123")).thenReturn(downloadResponse);

            mockMvc.perform(get("/api/reports/report-123/download"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reportId").value("report-123"))
                    .andExpect(jsonPath("$.fileName").value("Monthly Summary.pdf"))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"))
                    .andExpect(jsonPath("$.fileSizeBytes").value(1024))
                    .andExpect(jsonPath("$.downloadUrl").value("/reports/monthly-summary.pdf"));
        }

        @Test
        @DisplayName("should return 404 when report not found")
        void downloadReport_notFound() throws Exception {
            when(reportService.downloadReport("nonexistent"))
                    .thenThrow(new ReportNotFoundException("nonexistent"));

            mockMvc.perform(get("/api/reports/nonexistent/download"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 500 when report not ready")
        void downloadReport_notReady() throws Exception {
            when(reportService.downloadReport("report-123"))
                    .thenThrow(new ReportServiceException("Report is not ready for download. Current status: IN_PROGRESS", "REPORT_NOT_READY"));

            mockMvc.perform(get("/api/reports/report-123/download"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("REPORT_NOT_READY"));
        }
    }

    @Nested
    @DisplayName("GET /api/reports")
    class ListReportsEndpoint {

        @Test
        @DisplayName("should return paginated reports with default parameters")
        void listReports_defaultParams() throws Exception {
            Page<ReportResponse> page = new PageImpl<>(List.of(sampleResponse));
            when(reportService.listReports(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/reports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].reportId").value("report-123"));
        }

        @Test
        @DisplayName("should pass filter parameters to service")
        void listReports_withFilters() throws Exception {
            Page<ReportResponse> page = new PageImpl<>(List.of(sampleResponse));
            when(reportService.listReports(
                    eq(ReportType.MONTHLY_SUMMARY),
                    eq(ReportStatus.COMPLETED),
                    eq("user1"),
                    any(Pageable.class)
            )).thenReturn(page);

            mockMvc.perform(get("/api/reports")
                            .param("type", "MONTHLY_SUMMARY")
                            .param("status", "COMPLETED")
                            .param("createdBy", "user1")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(reportService).listReports(
                    eq(ReportType.MONTHLY_SUMMARY),
                    eq(ReportStatus.COMPLETED),
                    eq("user1"),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("should return empty page when no reports match")
        void listReports_empty() throws Exception {
            Page<ReportResponse> emptyPage = new PageImpl<>(List.of());
            when(reportService.listReports(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/api/reports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("DELETE /api/reports/{id}")
    class DeleteReportEndpoint {

        @Test
        @DisplayName("should return 204 No Content on successful deletion")
        void deleteReport_success() throws Exception {
            doNothing().when(reportService).deleteReport("report-123");

            mockMvc.perform(delete("/api/reports/report-123"))
                    .andExpect(status().isNoContent());

            verify(reportService).deleteReport("report-123");
        }

        @Test
        @DisplayName("should return 404 when report not found")
        void deleteReport_notFound() throws Exception {
            doThrow(new ReportNotFoundException("nonexistent"))
                    .when(reportService).deleteReport("nonexistent");

            mockMvc.perform(delete("/api/reports/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 500 when report is in progress")
        void deleteReport_inProgress() throws Exception {
            doThrow(new ReportServiceException("Cannot delete a report that is currently being generated", "DELETE_IN_PROGRESS"))
                    .when(reportService).deleteReport("report-123");

            mockMvc.perform(delete("/api/reports/report-123"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("DELETE_IN_PROGRESS"));
        }
    }
}
