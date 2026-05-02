package com.fintech.schedulerservice.service;

import com.fintech.schedulerservice.dto.RecurringPaymentRequest;
import com.fintech.schedulerservice.dto.RecurringPaymentResponse;
import com.fintech.schedulerservice.dto.RecurringPaymentUpdateRequest;
import com.fintech.schedulerservice.entity.PaymentFrequency;
import com.fintech.schedulerservice.entity.RecurringPayment;
import com.fintech.schedulerservice.entity.RecurringPaymentStatus;
import com.fintech.schedulerservice.exception.InvalidJobStateException;
import com.fintech.schedulerservice.exception.JobNotFoundException;
import com.fintech.schedulerservice.exception.JobSchedulingException;
import com.fintech.schedulerservice.repository.RecurringPaymentRepository;
import com.fintech.schedulerservice.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringPaymentServiceTest {

    @Mock
    private RecurringPaymentRepository recurringPaymentRepository;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Mock
    private Scheduler quartzScheduler;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private RecurringPaymentService recurringPaymentService;

    private RecurringPayment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = new RecurringPayment();
        samplePayment.setId("pay-001");
        samplePayment.setUserId("user-123");
        samplePayment.setSourceAccountId("acc-src-001");
        samplePayment.setDestinationAccountId("acc-dst-001");
        samplePayment.setAmount(new BigDecimal("100.00"));
        samplePayment.setCurrency("USD");
        samplePayment.setFrequency(PaymentFrequency.MONTHLY);
        samplePayment.setStartDate(LocalDate.now());
        samplePayment.setEndDate(LocalDate.now().plusYears(1));
        samplePayment.setStatus(RecurringPaymentStatus.ACTIVE);
        samplePayment.setMaxRetries(3);
        samplePayment.setCurrentRetryCount(0);
        samplePayment.setDescription("Monthly rent");
        samplePayment.setNextExecutionDate(LocalDate.now());
    }

    @Test
    void createRecurringPayment_shouldCreateAndSchedule() throws Exception {
        RecurringPaymentRequest request = new RecurringPaymentRequest(
                "user-123", "acc-src-001", "acc-dst-001",
                new BigDecimal("100.00"), "USD", PaymentFrequency.MONTHLY,
                LocalDate.now(), LocalDate.now().plusYears(1), "Monthly rent", 3
        );

        when(snowflakeIdGenerator.nextId()).thenReturn("pay-001");
        when(recurringPaymentRepository.save(any(RecurringPayment.class))).thenReturn(samplePayment);
        when(quartzScheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(quartzScheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenReturn(new java.util.Date());

        RecurringPaymentResponse response = recurringPaymentService.createRecurringPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("pay-001");
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getStatus()).isEqualTo(RecurringPaymentStatus.ACTIVE);
        verify(recurringPaymentRepository).save(any(RecurringPayment.class));
        verify(quartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void createRecurringPayment_whenQuartzFails_shouldThrow() throws Exception {
        RecurringPaymentRequest request = new RecurringPaymentRequest(
                "user-123", "acc-src-001", "acc-dst-001",
                new BigDecimal("100.00"), "USD", PaymentFrequency.MONTHLY,
                LocalDate.now(), null, "Monthly rent", null
        );

        when(snowflakeIdGenerator.nextId()).thenReturn("pay-001");
        when(recurringPaymentRepository.save(any(RecurringPayment.class))).thenReturn(samplePayment);
        when(quartzScheduler.checkExists(any(JobKey.class))).thenThrow(new SchedulerException("Quartz error"));

        assertThatThrownBy(() -> recurringPaymentService.createRecurringPayment(request))
                .isInstanceOf(JobSchedulingException.class);
    }

    @Test
    void getPaymentById_shouldReturnPaymentWhenExists() {
        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));

        RecurringPaymentResponse response = recurringPaymentService.getPaymentById("pay-001");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("pay-001");
        assertThat(response.getCurrency()).isEqualTo("USD");
    }

    @Test
    void getPaymentById_shouldThrowWhenNotExists() {
        when(recurringPaymentRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringPaymentService.getPaymentById("not-found"))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void getPaymentsByUserId_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<RecurringPayment> page = new PageImpl<>(List.of(samplePayment), pageable, 1);
        when(recurringPaymentRepository.findByUserId("user-123", pageable)).thenReturn(page);

        Page<RecurringPaymentResponse> result = recurringPaymentService.getPaymentsByUserId("user-123", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo("user-123");
    }

    @Test
    void getAllPayments_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<RecurringPayment> page = new PageImpl<>(List.of(samplePayment), pageable, 1);
        when(recurringPaymentRepository.findAll(pageable)).thenReturn(page);

        Page<RecurringPaymentResponse> result = recurringPaymentService.getAllPayments(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void updatePayment_shouldUpdateFieldsSuccessfully() throws Exception {
        RecurringPaymentUpdateRequest updateRequest = new RecurringPaymentUpdateRequest(
                new BigDecimal("200.00"), "EUR", null, "acc-dst-002",
                LocalDate.now().plusYears(2), "Updated description", 5
        );

        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));
        when(recurringPaymentRepository.save(any(RecurringPayment.class))).thenReturn(samplePayment);

        RecurringPaymentResponse response = recurringPaymentService.updatePayment("pay-001", updateRequest);

        assertThat(response).isNotNull();
        verify(recurringPaymentRepository).save(any(RecurringPayment.class));
    }

    @Test
    void updatePayment_withFrequencyChange_shouldReschedule() throws Exception {
        RecurringPaymentUpdateRequest updateRequest = new RecurringPaymentUpdateRequest(
                null, null, PaymentFrequency.WEEKLY, null, null, null, null
        );

        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));
        when(recurringPaymentRepository.save(any(RecurringPayment.class))).thenReturn(samplePayment);
        when(quartzScheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(quartzScheduler.deleteJob(any(JobKey.class))).thenReturn(true);
        when(quartzScheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenReturn(new java.util.Date());

        recurringPaymentService.updatePayment("pay-001", updateRequest);

        verify(quartzScheduler, atLeastOnce()).deleteJob(any(JobKey.class));
        verify(quartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void updatePayment_shouldThrowWhenNotFound() {
        RecurringPaymentUpdateRequest updateRequest = new RecurringPaymentUpdateRequest(
                new BigDecimal("200.00"), null, null, null, null, null, null
        );

        when(recurringPaymentRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringPaymentService.updatePayment("not-found", updateRequest))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void updatePayment_shouldThrowWhenCancelled() {
        samplePayment.setStatus(RecurringPaymentStatus.CANCELLED);
        RecurringPaymentUpdateRequest updateRequest = new RecurringPaymentUpdateRequest(
                new BigDecimal("200.00"), null, null, null, null, null, null
        );

        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> recurringPaymentService.updatePayment("pay-001", updateRequest))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void pausePayment_shouldPauseActivePayment() throws Exception {
        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));
        when(recurringPaymentRepository.save(any(RecurringPayment.class))).thenReturn(samplePayment);

        RecurringPaymentResponse response = recurringPaymentService.pausePayment("pay-001");

        assertThat(response).isNotNull();
        verify(quartzScheduler).pauseJob(any(JobKey.class));
        verify(recurringPaymentRepository).save(any(RecurringPayment.class));
    }

    @Test
    void pausePayment_shouldThrowWhenNotActive() {
        samplePayment.setStatus(RecurringPaymentStatus.PAUSED);
        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> recurringPaymentService.pausePayment("pay-001"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void pausePayment_shouldThrowWhenNotFound() {
        when(recurringPaymentRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringPaymentService.pausePayment("not-found"))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void resumePayment_shouldResumePausedPayment() throws Exception {
        samplePayment.setStatus(RecurringPaymentStatus.PAUSED);
        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));
        when(recurringPaymentRepository.save(any(RecurringPayment.class))).thenReturn(samplePayment);

        RecurringPaymentResponse response = recurringPaymentService.resumePayment("pay-001");

        assertThat(response).isNotNull();
        verify(quartzScheduler).resumeJob(any(JobKey.class));
        verify(recurringPaymentRepository).save(any(RecurringPayment.class));
    }

    @Test
    void resumePayment_shouldThrowWhenNotPaused() {
        samplePayment.setStatus(RecurringPaymentStatus.ACTIVE);
        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> recurringPaymentService.resumePayment("pay-001"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void resumePayment_shouldThrowWhenNotFound() {
        when(recurringPaymentRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringPaymentService.resumePayment("not-found"))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void cancelPayment_shouldCancelAndUnschedule() throws Exception {
        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));
        when(recurringPaymentRepository.save(any(RecurringPayment.class))).thenReturn(samplePayment);
        when(quartzScheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(quartzScheduler.deleteJob(any(JobKey.class))).thenReturn(true);

        recurringPaymentService.cancelPayment("pay-001");

        verify(recurringPaymentRepository).save(any(RecurringPayment.class));
        verify(quartzScheduler).deleteJob(any(JobKey.class));
    }

    @Test
    void cancelPayment_shouldThrowWhenAlreadyCancelled() {
        samplePayment.setStatus(RecurringPaymentStatus.CANCELLED);
        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> recurringPaymentService.cancelPayment("pay-001"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void cancelPayment_shouldThrowWhenCompleted() {
        samplePayment.setStatus(RecurringPaymentStatus.COMPLETED);
        when(recurringPaymentRepository.findById("pay-001")).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> recurringPaymentService.cancelPayment("pay-001"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void cancelPayment_shouldThrowWhenNotFound() {
        when(recurringPaymentRepository.findById("not-found")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurringPaymentService.cancelPayment("not-found"))
                .isInstanceOf(JobNotFoundException.class);
    }
}
