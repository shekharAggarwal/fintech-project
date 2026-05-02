package com.fintech.retryservice.service;

import com.fintech.retryservice.dto.RetryRequest;
import com.fintech.retryservice.dto.RetryResponse;
import com.fintech.retryservice.dto.RetryStatusUpdate;
import com.fintech.retryservice.model.RetryAttempt;
import com.fintech.retryservice.model.RetryStatus;
import com.fintech.retryservice.model.RetryType;
import com.fintech.retryservice.repository.RetryAttemptRepository;
import com.fintech.retryservice.util.SnowflakeIdGenerator;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock
    private RetryAttemptRepository retryAttemptRepository;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private RetryService retryService;

    private RetryAttempt sampleAttempt;

    @BeforeEach
    void setUp() {
        sampleAttempt = new RetryAttempt();
        sampleAttempt.setRetryId("retry-123");
        sampleAttempt.setOriginalId("orig-456");
        sampleAttempt.setRetryType(RetryType.PAYMENT_PROCESSING);
        sampleAttempt.setRetryStatus(RetryStatus.PENDING);
        sampleAttempt.setRetryCount(0);
        sampleAttempt.setMaxRetries(3);
        sampleAttempt.setRetryDelaySeconds(60);
        sampleAttempt.setNextRetryTime(LocalDateTime.now().plusSeconds(60));
        sampleAttempt.setServiceName("payment-service");
        sampleAttempt.setCreatedBy("test-user");
        sampleAttempt.setPriority("NORMAL");
    }

    @Nested
    @DisplayName("scheduleRetry")
    class ScheduleRetryTests {

        @Test
        @DisplayName("should create retry with default values when optionals are null")
        void scheduleRetry_withDefaults() {
            RetryRequest request = new RetryRequest();
            request.setOriginalId("orig-456");
            request.setRetryType(RetryType.PAYMENT_PROCESSING);
            request.setServiceName("payment-service");
            request.setCreatedBy("test-user");

            when(idGenerator.nextId()).thenReturn("generated-id");
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            RetryResponse response = retryService.scheduleRetry(request);

            assertThat(response.getRetryId()).isEqualTo("generated-id");
            assertThat(response.getOriginalId()).isEqualTo("orig-456");
            assertThat(response.getRetryStatus()).isEqualTo(RetryStatus.PENDING);
            assertThat(response.getMaxRetries()).isEqualTo(3);
            assertThat(response.getRetryDelaySeconds()).isEqualTo(60);
            assertThat(response.getPriority()).isEqualTo("NORMAL");
            assertThat(response.getRetryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should create retry with custom values when provided")
        void scheduleRetry_withCustomValues() {
            RetryRequest request = new RetryRequest();
            request.setOriginalId("orig-789");
            request.setRetryType(RetryType.NOTIFICATION_DELIVERY);
            request.setServiceName("notification-service");
            request.setCreatedBy("admin");
            request.setMaxRetries(5);
            request.setRetryDelaySeconds(120);
            request.setPriority("HIGH");
            request.setEndpointUrl("http://example.com/retry");
            request.setRetryData(Map.of("key", "value"));

            when(idGenerator.nextId()).thenReturn("custom-id");
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            RetryResponse response = retryService.scheduleRetry(request);

            assertThat(response.getMaxRetries()).isEqualTo(5);
            assertThat(response.getRetryDelaySeconds()).isEqualTo(120);
            assertThat(response.getPriority()).isEqualTo("HIGH");
            assertThat(response.getEndpointUrl()).isEqualTo("http://example.com/retry");
            assertThat(response.getRetryData()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("should use provided nextRetryTime when set")
        void scheduleRetry_withCustomNextRetryTime() {
            LocalDateTime customTime = LocalDateTime.of(2025, 1, 1, 12, 0);
            RetryRequest request = new RetryRequest();
            request.setOriginalId("orig-100");
            request.setRetryType(RetryType.LEDGER_SYNC);
            request.setServiceName("ledger-service");
            request.setCreatedBy("system");
            request.setNextRetryTime(customTime);

            when(idGenerator.nextId()).thenReturn("time-id");
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            RetryResponse response = retryService.scheduleRetry(request);

            assertThat(response.getNextRetryTime()).isEqualTo(customTime);
        }
    }

    @Nested
    @DisplayName("handleResult")
    class HandleResultTests {

        @Test
        @DisplayName("should mark retry as COMPLETED on success")
        void handleResult_success() {
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            retryService.handleResult("retry-123", true, null);

            ArgumentCaptor<RetryAttempt> captor = ArgumentCaptor.forClass(RetryAttempt.class);
            verify(retryAttemptRepository).save(captor.capture());
            RetryAttempt saved = captor.getValue();
            assertThat(saved.getRetryStatus()).isEqualTo(RetryStatus.COMPLETED);
            assertThat(saved.getCompletedAt()).isNotNull();
            assertThat(saved.getLastUpdatedBy()).isEqualTo("retry-service");
        }

        @Test
        @DisplayName("should increment retry count and reschedule on failure")
        void handleResult_failure_belowMax() {
            sampleAttempt.setRetryCount(0);
            sampleAttempt.setMaxRetries(3);
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            retryService.handleResult("retry-123", false, "Connection timeout");

            ArgumentCaptor<RetryAttempt> captor = ArgumentCaptor.forClass(RetryAttempt.class);
            verify(retryAttemptRepository).save(captor.capture());
            RetryAttempt saved = captor.getValue();
            assertThat(saved.getRetryCount()).isEqualTo(1);
            assertThat(saved.getRetryStatus()).isEqualTo(RetryStatus.PENDING);
            assertThat(saved.getErrorMessage()).isEqualTo("Connection timeout");
            assertThat(saved.getLastErrorCode()).isEqualTo("RETRY_FAILED");
        }

        @Test
        @DisplayName("should set MAX_RETRIES_EXCEEDED when max retries reached on failure")
        void handleResult_failure_maxRetriesExceeded() {
            sampleAttempt.setRetryCount(2);
            sampleAttempt.setMaxRetries(3);
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            retryService.handleResult("retry-123", false, "Still failing");

            ArgumentCaptor<RetryAttempt> captor = ArgumentCaptor.forClass(RetryAttempt.class);
            verify(retryAttemptRepository).save(captor.capture());
            RetryAttempt saved = captor.getValue();
            assertThat(saved.getRetryCount()).isEqualTo(3);
            assertThat(saved.getRetryStatus()).isEqualTo(RetryStatus.MAX_RETRIES_EXCEEDED);
        }

        @Test
        @DisplayName("should not modify COMPLETED attempts")
        void handleResult_terminalState_completed() {
            sampleAttempt.setRetryStatus(RetryStatus.COMPLETED);
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));

            retryService.handleResult("retry-123", false, "error");

            verify(retryAttemptRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not modify CANCELLED attempts")
        void handleResult_terminalState_cancelled() {
            sampleAttempt.setRetryStatus(RetryStatus.CANCELLED);
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));

            retryService.handleResult("retry-123", false, "error");

            verify(retryAttemptRepository, never()).save(any());
        }

        @Test
        @DisplayName("should do nothing when retry not found")
        void handleResult_notFound() {
            when(retryAttemptRepository.findById("nonexistent")).thenReturn(Optional.empty());

            retryService.handleResult("nonexistent", true, null);

            verify(retryAttemptRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Exponential backoff (model layer)")
    class ExponentialBackoffTests {

        @Test
        @DisplayName("first retry uses base delay")
        void firstRetry_baseDelay() {
            RetryAttempt attempt = new RetryAttempt();
            attempt.setRetryCount(0);
            attempt.setMaxRetries(5);
            attempt.setRetryDelaySeconds(60);
            attempt.setRetryStatus(RetryStatus.PENDING);

            LocalDateTime before = LocalDateTime.now();
            attempt.incrementRetryCount();
            LocalDateTime after = LocalDateTime.now();

            assertThat(attempt.getRetryCount()).isEqualTo(1);
            // backoff = 60 * 2^0 = 60 seconds
            assertThat(attempt.getNextRetryTime()).isAfter(before.plusSeconds(59));
            assertThat(attempt.getNextRetryTime()).isBefore(after.plusSeconds(61));
        }

        @Test
        @DisplayName("second retry doubles delay")
        void secondRetry_doubledDelay() {
            RetryAttempt attempt = new RetryAttempt();
            attempt.setRetryCount(1);
            attempt.setMaxRetries(5);
            attempt.setRetryDelaySeconds(60);
            attempt.setRetryStatus(RetryStatus.PENDING);

            LocalDateTime before = LocalDateTime.now();
            attempt.incrementRetryCount();

            assertThat(attempt.getRetryCount()).isEqualTo(2);
            // backoff = 60 * 2^1 = 120 seconds
            assertThat(attempt.getNextRetryTime()).isAfter(before.plusSeconds(119));
        }

        @Test
        @DisplayName("third retry quadruples delay")
        void thirdRetry_quadrupledDelay() {
            RetryAttempt attempt = new RetryAttempt();
            attempt.setRetryCount(2);
            attempt.setMaxRetries(5);
            attempt.setRetryDelaySeconds(60);
            attempt.setRetryStatus(RetryStatus.PENDING);

            LocalDateTime before = LocalDateTime.now();
            attempt.incrementRetryCount();

            assertThat(attempt.getRetryCount()).isEqualTo(3);
            // backoff = 60 * 2^2 = 240 seconds
            assertThat(attempt.getNextRetryTime()).isAfter(before.plusSeconds(239));
        }

        @Test
        @DisplayName("sets MAX_RETRIES_EXCEEDED when max reached")
        void maxRetriesExceeded() {
            RetryAttempt attempt = new RetryAttempt();
            attempt.setRetryCount(2);
            attempt.setMaxRetries(3);
            attempt.setRetryDelaySeconds(60);
            attempt.setRetryStatus(RetryStatus.PENDING);

            attempt.incrementRetryCount();

            assertThat(attempt.getRetryCount()).isEqualTo(3);
            assertThat(attempt.getRetryStatus()).isEqualTo(RetryStatus.MAX_RETRIES_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("forceRetryNow")
    class ForceRetryNowTests {

        @Test
        @DisplayName("should set status to PENDING and nextRetryTime to now")
        void forceRetryNow_success() {
            sampleAttempt.setRetryStatus(RetryStatus.IN_PROGRESS);
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            RetryResponse response = retryService.forceRetryNow("retry-123");

            assertThat(response.getRetryStatus()).isEqualTo(RetryStatus.PENDING);
            assertThat(response.getNextRetryTime()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("should throw NoSuchElementException when not found")
        void forceRetryNow_notFound() {
            when(retryAttemptRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> retryService.forceRetryNow("missing"))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException for COMPLETED tasks")
        void forceRetryNow_completed() {
            sampleAttempt.setRetryStatus(RetryStatus.COMPLETED);
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));

            assertThatThrownBy(() -> retryService.forceRetryNow("retry-123"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException for CANCELLED tasks")
        void forceRetryNow_cancelled() {
            sampleAttempt.setRetryStatus(RetryStatus.CANCELLED);
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));

            assertThatThrownBy(() -> retryService.forceRetryNow("retry-123"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("cancelRetry")
    class CancelRetryTests {

        @Test
        @DisplayName("should cancel a PENDING retry")
        void cancelRetry_pending() {
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            RetryResponse response = retryService.cancelRetry("retry-123");

            assertThat(response.getRetryStatus()).isEqualTo(RetryStatus.CANCELLED);
        }

        @Test
        @DisplayName("should throw NoSuchElementException when not found")
        void cancelRetry_notFound() {
            when(retryAttemptRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> retryService.cancelRetry("missing"))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException for COMPLETED tasks")
        void cancelRetry_completed() {
            sampleAttempt.setRetryStatus(RetryStatus.COMPLETED);
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));

            assertThatThrownBy(() -> retryService.cancelRetry("retry-123"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("resetStuckRetries")
    class ResetStuckRetriesTests {

        @Test
        @DisplayName("should call repository to reset stuck retries")
        void resetStuckRetries_updatesRecords() {
            when(retryAttemptRepository.updateStuckRetryAttempts(
                    eq(RetryStatus.IN_PROGRESS), eq(RetryStatus.PENDING),
                    any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(5);

            int updated = retryService.resetStuckRetries();

            assertThat(updated).isEqualTo(5);
            verify(retryAttemptRepository).updateStuckRetryAttempts(
                    eq(RetryStatus.IN_PROGRESS), eq(RetryStatus.PENDING),
                    any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("should return 0 when no stuck retries")
        void resetStuckRetries_noRecords() {
            when(retryAttemptRepository.updateStuckRetryAttempts(
                    any(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(0);

            int updated = retryService.resetStuckRetries();

            assertThat(updated).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getRetryById")
    class GetRetryByIdTests {

        @Test
        @DisplayName("should return response for existing retry")
        void getRetryById_found() {
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));

            RetryResponse response = retryService.getRetryById("retry-123");

            assertThat(response.getRetryId()).isEqualTo("retry-123");
            assertThat(response.getOriginalId()).isEqualTo("orig-456");
        }

        @Test
        @DisplayName("should throw NoSuchElementException when not found")
        void getRetryById_notFound() {
            when(retryAttemptRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> retryService.getRetryById("missing"))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("getRetriesByStatus / getAllRetries")
    class PaginationTests {

        @Test
        @DisplayName("should return filtered page by status")
        void getRetriesByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<RetryAttempt> page = new PageImpl<>(List.of(sampleAttempt), pageable, 1);
            when(retryAttemptRepository.findByRetryStatus(RetryStatus.PENDING, pageable)).thenReturn(page);

            Page<RetryResponse> result = retryService.getRetriesByStatus(RetryStatus.PENDING, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getRetryId()).isEqualTo("retry-123");
        }

        @Test
        @DisplayName("should return all retries paginated")
        void getAllRetries() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<RetryAttempt> page = new PageImpl<>(List.of(sampleAttempt), pageable, 1);
            when(retryAttemptRepository.findAll(pageable)).thenReturn(page);

            Page<RetryResponse> result = retryService.getAllRetries(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("should update all provided fields")
        void updateStatus_allFields() {
            when(retryAttemptRepository.findById("retry-123")).thenReturn(Optional.of(sampleAttempt));
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            RetryStatusUpdate update = new RetryStatusUpdate();
            update.setRetryStatus(RetryStatus.COMPLETED);
            update.setErrorMessage("Fixed");
            update.setLastErrorCode("RESOLVED");
            update.setCompletedAt(LocalDateTime.of(2025, 6, 1, 12, 0));
            update.setUpdatedBy("callback-service");

            RetryResponse response = retryService.updateStatus("retry-123", update);

            assertThat(response.getRetryStatus()).isEqualTo(RetryStatus.COMPLETED);
            assertThat(response.getErrorMessage()).isEqualTo("Fixed");
            assertThat(response.getLastErrorCode()).isEqualTo("RESOLVED");
            assertThat(response.getCompletedAt()).isEqualTo(LocalDateTime.of(2025, 6, 1, 12, 0));
        }

        @Test
        @DisplayName("should throw NoSuchElementException when not found")
        void updateStatus_notFound() {
            when(retryAttemptRepository.findById("missing")).thenReturn(Optional.empty());
            RetryStatusUpdate update = new RetryStatusUpdate();
            update.setRetryStatus(RetryStatus.FAILED);
            update.setUpdatedBy("system");

            assertThatThrownBy(() -> retryService.updateStatus("missing", update))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("getStatistics")
    class GetStatisticsTests {

        @Test
        @DisplayName("should return statistics map with counts and breakdown")
        void getStatistics() {
            when(retryAttemptRepository.countByRetryStatus(RetryStatus.PENDING)).thenReturn(10L);
            when(retryAttemptRepository.countByRetryStatus(RetryStatus.IN_PROGRESS)).thenReturn(2L);
            when(retryAttemptRepository.countByRetryStatus(RetryStatus.COMPLETED)).thenReturn(50L);
            when(retryAttemptRepository.countByRetryStatus(RetryStatus.FAILED)).thenReturn(3L);
            when(retryAttemptRepository.countByRetryStatus(RetryStatus.CANCELLED)).thenReturn(1L);
            when(retryAttemptRepository.countByRetryStatus(RetryStatus.MAX_RETRIES_EXCEEDED)).thenReturn(4L);
            when(retryAttemptRepository.getRetryStatistics()).thenReturn(List.of());

            Map<String, Object> stats = retryService.getStatistics();

            assertThat(stats.get("pending")).isEqualTo(10L);
            assertThat(stats.get("inProgress")).isEqualTo(2L);
            assertThat(stats.get("completed")).isEqualTo(50L);
            assertThat(stats.get("failed")).isEqualTo(3L);
            assertThat(stats.get("cancelled")).isEqualTo(1L);
            assertThat(stats.get("maxRetriesExceeded")).isEqualTo(4L);
            assertThat(stats).containsKey("breakdown");
            assertThat(stats).containsKey("timestamp");
        }
    }

    @Nested
    @DisplayName("executeRetries")
    class ExecuteRetriesTests {

        @Test
        @DisplayName("should do nothing when no retries are due")
        void executeRetries_noDue() {
            when(retryAttemptRepository.findRetryAttemptsReadyForExecution(
                    eq(RetryStatus.PENDING), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            retryService.executeRetries();

            verify(retryAttemptRepository, never()).save(any());
        }

        @Test
        @DisplayName("should dispatch via Kafka when no endpoint URL")
        void executeRetries_kafka() {
            sampleAttempt.setEndpointUrl(null);
            when(retryAttemptRepository.findRetryAttemptsReadyForExecution(
                    eq(RetryStatus.PENDING), any(LocalDateTime.class)))
                    .thenReturn(List.of(sampleAttempt));
            when(retryAttemptRepository.save(any(RetryAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

            retryService.executeRetries();

            verify(kafkaTemplate).send(anyString(), eq("orig-456"), any());
            verify(retryAttemptRepository, atLeastOnce()).save(any());
        }
    }

    @Nested
    @DisplayName("cleanupOldRecords")
    class CleanupTests {

        @Test
        @DisplayName("should call repository deleteOldRetryAttempts")
        void cleanupOldRecords() {
            retryService.cleanupOldRecords();

            verify(retryAttemptRepository).deleteOldRetryAttempts(
                    eq(List.of(RetryStatus.COMPLETED, RetryStatus.CANCELLED, RetryStatus.MAX_RETRIES_EXCEEDED)),
                    any(LocalDateTime.class));
        }
    }
}
