package com.fintech.reportingservice.service;

import com.fintech.reportingservice.dto.request.GenerateReportRequest;
import com.fintech.reportingservice.dto.response.ReportDownloadResponse;
import com.fintech.reportingservice.dto.response.ReportResponse;
import com.fintech.reportingservice.entity.Report;
import com.fintech.reportingservice.exception.ReportNotFoundException;
import com.fintech.reportingservice.exception.ReportServiceException;
import com.fintech.reportingservice.model.ReportFormat;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.model.ReportType;
import com.fintech.reportingservice.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportGenerationWorker reportGenerationWorker;

    @InjectMocks
    private ReportService reportService;

    private Report sampleReport;

    @BeforeEach
    void setUp() {
        sampleReport = new Report();
        sampleReport.setReportId("test-report-id");
        sampleReport.setReportName("Monthly Summary");
        sampleReport.setReportType(ReportType.MONTHLY_SUMMARY);
        sampleReport.setReportFormat(ReportFormat.PDF);
        sampleReport.setReportStatus(ReportStatus.PENDING);
        sampleReport.setCreatedBy("user1");
        sampleReport.setDescription("Test description");
        sampleReport.setExpiresAt(LocalDateTime.now().plusDays(30));
        sampleReport.setCreatedAt(LocalDateTime.now());
        sampleReport.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("generateReport")
    class GenerateReportTests {

        @Test
        @DisplayName("should create report with PENDING status and trigger async generation")
        void generateReport_success() {
            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportName("Monthly Summary");
            request.setReportType(ReportType.MONTHLY_SUMMARY);
            request.setReportFormat(ReportFormat.PDF);
            request.setDescription("Test description");

            when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
                Report r = invocation.getArgument(0);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            ReportResponse response = reportService.generateReport(request, "user1");

            assertThat(response).isNotNull();
            assertThat(response.getReportName()).isEqualTo("Monthly Summary");
            assertThat(response.getReportType()).isEqualTo(ReportType.MONTHLY_SUMMARY);
            assertThat(response.getReportFormat()).isEqualTo(ReportFormat.PDF);
            assertThat(response.getReportStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(response.getCreatedBy()).isEqualTo("user1");
            assertThat(response.getReportId()).isNotNull();

            verify(reportRepository).save(any(Report.class));
            verify(reportGenerationWorker).processReportGeneration(any(String.class));
        }

        @Test
        @DisplayName("should set status to SCHEDULED when scheduledAt is in the future")
        void generateReport_scheduled() {
            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportName("Scheduled Report");
            request.setReportType(ReportType.TRANSACTION_SUMMARY);
            request.setReportFormat(ReportFormat.CSV);
            request.setScheduledAt(LocalDateTime.now().plusHours(2));

            when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
                Report r = invocation.getArgument(0);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            ReportResponse response = reportService.generateReport(request, "user2");

            assertThat(response.getReportStatus()).isEqualTo(ReportStatus.SCHEDULED);
            verify(reportGenerationWorker, never()).processReportGeneration(any());
        }

        @Test
        @DisplayName("should store parameters when provided")
        void generateReport_withParameters() {
            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportName("Parameterized Report");
            request.setReportType(ReportType.CUSTOM_QUERY);
            request.setReportFormat(ReportFormat.JSON);
            request.setParameters(Map.of("startDate", "2024-01-01", "endDate", "2024-01-31"));

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            when(reportRepository.save(captor.capture())).thenAnswer(invocation -> {
                Report r = invocation.getArgument(0);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            reportService.generateReport(request, "user3");

            Report saved = captor.getValue();
            assertThat(saved.getParameters()).containsEntry("startDate", "2024-01-01");
            assertThat(saved.getParameters()).containsEntry("endDate", "2024-01-31");
        }

        @Test
        @DisplayName("should set expiry date 30 days from now")
        void generateReport_setsExpiry() {
            GenerateReportRequest request = new GenerateReportRequest();
            request.setReportName("Expiry Test");
            request.setReportType(ReportType.ACCOUNT_STATEMENT);
            request.setReportFormat(ReportFormat.EXCEL);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            when(reportRepository.save(captor.capture())).thenAnswer(invocation -> {
                Report r = invocation.getArgument(0);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            reportService.generateReport(request, "user1");

            Report saved = captor.getValue();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
            assertThat(saved.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(31));
        }
    }

    @Nested
    @DisplayName("getReportStatus")
    class GetReportStatusTests {

        @Test
        @DisplayName("should return report response when found")
        void getReportStatus_found() {
            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));

            ReportResponse response = reportService.getReportStatus("test-report-id");

            assertThat(response).isNotNull();
            assertThat(response.getReportId()).isEqualTo("test-report-id");
            assertThat(response.getReportName()).isEqualTo("Monthly Summary");
            assertThat(response.getReportStatus()).isEqualTo(ReportStatus.PENDING);
        }

        @Test
        @DisplayName("should throw ReportNotFoundException when not found")
        void getReportStatus_notFound() {
            when(reportRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.getReportStatus("nonexistent"))
                    .isInstanceOf(ReportNotFoundException.class)
                    .hasMessageContaining("nonexistent");
        }
    }

    @Nested
    @DisplayName("downloadReport")
    class DownloadReportTests {

        @Test
        @DisplayName("should return download response for completed report")
        void downloadReport_success() {
            sampleReport.setReportStatus(ReportStatus.COMPLETED);
            sampleReport.setFilePath("/reports/monthly-summary.pdf");
            sampleReport.setFileSizeBytes(1024L);
            sampleReport.setExpiresAt(LocalDateTime.now().plusDays(10));

            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));

            ReportDownloadResponse response = reportService.downloadReport("test-report-id");

            assertThat(response.getReportId()).isEqualTo("test-report-id");
            assertThat(response.getReportName()).isEqualTo("Monthly Summary");
            assertThat(response.getFileName()).isEqualTo("Monthly Summary.pdf");
            assertThat(response.getContentType()).isEqualTo("application/pdf");
            assertThat(response.getFileSizeBytes()).isEqualTo(1024L);
            assertThat(response.getDownloadUrl()).isEqualTo("/reports/monthly-summary.pdf");

            verify(reportRepository).incrementDownloadCount("test-report-id");
        }

        @Test
        @DisplayName("should throw exception when report not found")
        void downloadReport_notFound() {
            when(reportRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.downloadReport("nonexistent"))
                    .isInstanceOf(ReportNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when report is expired")
        void downloadReport_expired() {
            sampleReport.setReportStatus(ReportStatus.COMPLETED);
            sampleReport.setFilePath("/reports/old-report.pdf");
            sampleReport.setExpiresAt(LocalDateTime.now().minusDays(1));

            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));

            assertThatThrownBy(() -> reportService.downloadReport("test-report-id"))
                    .isInstanceOf(ReportServiceException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("should throw exception when report is not completed")
        void downloadReport_notReady() {
            sampleReport.setReportStatus(ReportStatus.IN_PROGRESS);
            sampleReport.setExpiresAt(LocalDateTime.now().plusDays(10));

            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));

            assertThatThrownBy(() -> reportService.downloadReport("test-report-id"))
                    .isInstanceOf(ReportServiceException.class)
                    .hasMessageContaining("not ready");
        }

        @Test
        @DisplayName("should throw exception when file path is null")
        void downloadReport_fileUnavailable() {
            sampleReport.setReportStatus(ReportStatus.COMPLETED);
            sampleReport.setFilePath(null);
            sampleReport.setExpiresAt(LocalDateTime.now().plusDays(10));

            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));

            assertThatThrownBy(() -> reportService.downloadReport("test-report-id"))
                    .isInstanceOf(ReportServiceException.class)
                    .hasMessageContaining("not available");
        }

        @Test
        @DisplayName("should return correct content type for CSV format")
        void downloadReport_csvFormat() {
            sampleReport.setReportStatus(ReportStatus.COMPLETED);
            sampleReport.setReportFormat(ReportFormat.CSV);
            sampleReport.setFilePath("/reports/data.csv");
            sampleReport.setFileSizeBytes(512L);
            sampleReport.setExpiresAt(LocalDateTime.now().plusDays(10));

            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));

            ReportDownloadResponse response = reportService.downloadReport("test-report-id");

            assertThat(response.getContentType()).isEqualTo("text/csv");
            assertThat(response.getFileName()).isEqualTo("Monthly Summary.csv");
        }

        @Test
        @DisplayName("should return correct content type for EXCEL format")
        void downloadReport_excelFormat() {
            sampleReport.setReportStatus(ReportStatus.COMPLETED);
            sampleReport.setReportFormat(ReportFormat.EXCEL);
            sampleReport.setFilePath("/reports/data.xlsx");
            sampleReport.setFileSizeBytes(2048L);
            sampleReport.setExpiresAt(LocalDateTime.now().plusDays(10));

            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));

            ReportDownloadResponse response = reportService.downloadReport("test-report-id");

            assertThat(response.getContentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            assertThat(response.getFileName()).isEqualTo("Monthly Summary.xlsx");
        }
    }

    @Nested
    @DisplayName("listReports")
    class ListReportsTests {

        @Test
        @DisplayName("should return paginated reports with filters")
        void listReports_withFilters() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Report> page = new PageImpl<>(List.of(sampleReport), pageable, 1);

            when(reportRepository.findByFilters(
                    eq(ReportType.MONTHLY_SUMMARY),
                    eq(ReportStatus.PENDING),
                    eq("user1"),
                    any(Pageable.class)
            )).thenReturn(page);

            Page<ReportResponse> result = reportService.listReports(
                    ReportType.MONTHLY_SUMMARY, ReportStatus.PENDING, "user1", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getReportId()).isEqualTo("test-report-id");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page when no reports match")
        void listReports_empty() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Report> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(reportRepository.findByFilters(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<ReportResponse> result = reportService.listReports(null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("deleteReport")
    class DeleteReportTests {

        @Test
        @DisplayName("should mark report as CANCELLED")
        void deleteReport_success() {
            sampleReport.setReportStatus(ReportStatus.COMPLETED);
            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));
            when(reportRepository.save(any(Report.class))).thenReturn(sampleReport);

            reportService.deleteReport("test-report-id");

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(captor.capture());
            assertThat(captor.getValue().getReportStatus()).isEqualTo(ReportStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw exception when report not found")
        void deleteReport_notFound() {
            when(reportRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.deleteReport("nonexistent"))
                    .isInstanceOf(ReportNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when report is IN_PROGRESS")
        void deleteReport_inProgress() {
            sampleReport.setReportStatus(ReportStatus.IN_PROGRESS);
            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));

            assertThatThrownBy(() -> reportService.deleteReport("test-report-id"))
                    .isInstanceOf(ReportServiceException.class)
                    .hasMessageContaining("Cannot delete");
        }

        @Test
        @DisplayName("should allow deletion of PENDING report")
        void deleteReport_pending() {
            sampleReport.setReportStatus(ReportStatus.PENDING);
            when(reportRepository.findById("test-report-id")).thenReturn(Optional.of(sampleReport));
            when(reportRepository.save(any(Report.class))).thenReturn(sampleReport);

            reportService.deleteReport("test-report-id");

            verify(reportRepository).save(any(Report.class));
        }
    }
}
