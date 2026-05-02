package com.fintech.schedulerservice.service;

import com.fintech.schedulerservice.dto.JobRequest;
import com.fintech.schedulerservice.dto.JobResponse;
import com.fintech.schedulerservice.dto.JobStatusUpdate;
import com.fintech.schedulerservice.entity.JobStatus;
import com.fintech.schedulerservice.entity.JobType;
import com.fintech.schedulerservice.entity.ScheduledJob;
import com.fintech.schedulerservice.exception.InvalidJobStateException;
import com.fintech.schedulerservice.exception.JobNotFoundException;
import com.fintech.schedulerservice.exception.JobSchedulingException;
import com.fintech.schedulerservice.repository.ScheduledJobRepository;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock
    private ScheduledJobRepository scheduledJobRepository;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Mock
    private Scheduler quartzScheduler;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SchedulerService schedulerService;

    private ScheduledJob sampleJob;

    @BeforeEach
    void setUp() {
        sampleJob = new ScheduledJob(
                "12345",
                "Test Job",
                JobType.PAYMENT_RETRY,
                JobStatus.SCHEDULED,
                Instant.now().plusSeconds(3600),
                "A test job",
                "admin",
                "admin",
                "{\"key\":\"value\"}",
                0,
                3,
                60,
                "NORMAL"
        );
    }

    @Test
    void createJob_shouldCreateAndScheduleJob() throws Exception {
        JobRequest request = new JobRequest(
                "Test Job", JobType.PAYMENT_RETRY, LocalDateTime.now().plusHours(1),
                "A test job", "admin", null, 3, 60, "NORMAL"
        );

        when(snowflakeIdGenerator.nextId()).thenReturn("12345");
        when(scheduledJobRepository.save(any(ScheduledJob.class))).thenReturn(sampleJob);
        when(quartzScheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(quartzScheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenReturn(new java.util.Date());

        JobResponse response = schedulerService.createJob(request);

        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo("12345");
        assertThat(response.getJobName()).isEqualTo("Test Job");
        assertThat(response.getJobStatus()).isEqualTo(JobStatus.SCHEDULED);
        verify(scheduledJobRepository).save(any(ScheduledJob.class));
        verify(quartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void createJob_whenQuartzFails_shouldThrowJobSchedulingException() throws Exception {
        JobRequest request = new JobRequest(
                "Test Job", JobType.PAYMENT_RETRY, LocalDateTime.now().plusHours(1),
                "A test job", "admin", null, 3, 60, "NORMAL"
        );

        when(snowflakeIdGenerator.nextId()).thenReturn("12345");
        when(scheduledJobRepository.save(any(ScheduledJob.class))).thenReturn(sampleJob);
        when(quartzScheduler.checkExists(any(JobKey.class))).thenThrow(new SchedulerException("Quartz error"));

        assertThatThrownBy(() -> schedulerService.createJob(request))
                .isInstanceOf(JobSchedulingException.class);
    }

    @Test
    void getJobById_shouldReturnJobWhenExists() {
        when(scheduledJobRepository.findById("12345")).thenReturn(Optional.of(sampleJob));

        Optional<JobResponse> result = schedulerService.getJobById("12345");

        assertThat(result).isPresent();
        assertThat(result.get().getJobId()).isEqualTo("12345");
    }

    @Test
    void getJobById_shouldReturnEmptyWhenNotExists() {
        when(scheduledJobRepository.findById("99999")).thenReturn(Optional.empty());

        Optional<JobResponse> result = schedulerService.getJobById("99999");

        assertThat(result).isEmpty();
    }

    @Test
    void getJobsByStatus_shouldReturnFilteredJobs() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ScheduledJob> page = new PageImpl<>(List.of(sampleJob), pageable, 1);
        when(scheduledJobRepository.findByJobStatus(JobStatus.SCHEDULED, pageable)).thenReturn(page);

        Page<JobResponse> result = schedulerService.getJobsByStatus(JobStatus.SCHEDULED, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getJobStatus()).isEqualTo(JobStatus.SCHEDULED);
    }

    @Test
    void getAllJobs_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ScheduledJob> page = new PageImpl<>(List.of(sampleJob), pageable, 1);
        when(scheduledJobRepository.findAll(pageable)).thenReturn(page);

        Page<JobResponse> result = schedulerService.getAllJobs(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getJobsByType_shouldReturnJobsOfGivenType() {
        when(scheduledJobRepository.findByJobType(JobType.PAYMENT_RETRY)).thenReturn(List.of(sampleJob));

        List<JobResponse> result = schedulerService.getJobsByType(JobType.PAYMENT_RETRY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobType()).isEqualTo(JobType.PAYMENT_RETRY);
    }

    @Test
    void updateJobStatus_shouldUpdateStatusSuccessfully() {
        JobStatusUpdate update = new JobStatusUpdate(
                "12345", JobStatus.COMPLETED, "Success", null, null, "admin"
        );

        when(scheduledJobRepository.findById("12345")).thenReturn(Optional.of(sampleJob));
        when(scheduledJobRepository.save(any(ScheduledJob.class))).thenReturn(sampleJob);

        JobResponse response = schedulerService.updateJobStatus(update);

        assertThat(response).isNotNull();
        verify(scheduledJobRepository).save(any(ScheduledJob.class));
    }

    @Test
    void updateJobStatus_shouldThrowWhenJobNotFound() {
        JobStatusUpdate update = new JobStatusUpdate(
                "99999", JobStatus.COMPLETED, "Success", null, null, "admin"
        );

        when(scheduledJobRepository.findById("99999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulerService.updateJobStatus(update))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void updateJobStatus_toFailed_shouldScheduleRetryIfWithinLimits() throws Exception {
        sampleJob.setRetryCount(0);
        sampleJob.setMaxRetries(3);

        JobStatusUpdate update = new JobStatusUpdate(
                "12345", JobStatus.FAILED, null, "Error occurred", null, "admin"
        );

        when(scheduledJobRepository.findById("12345")).thenReturn(Optional.of(sampleJob));
        when(scheduledJobRepository.save(any(ScheduledJob.class))).thenReturn(sampleJob);
        when(quartzScheduler.checkExists(any(JobKey.class))).thenReturn(false);
        when(quartzScheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
                .thenReturn(new java.util.Date());

        schedulerService.updateJobStatus(update);

        verify(quartzScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void cancelJob_shouldCancelAndRemoveFromQuartz() throws Exception {
        when(scheduledJobRepository.findById("12345")).thenReturn(Optional.of(sampleJob));
        when(scheduledJobRepository.save(any(ScheduledJob.class))).thenReturn(sampleJob);
        when(quartzScheduler.deleteJob(any(JobKey.class))).thenReturn(true);

        JobResponse response = schedulerService.cancelJob("12345", "admin");

        assertThat(response).isNotNull();
        verify(quartzScheduler).deleteJob(any(JobKey.class));
        verify(scheduledJobRepository).save(any(ScheduledJob.class));
    }

    @Test
    void cancelJob_shouldThrowWhenJobNotFound() {
        when(scheduledJobRepository.findById("99999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulerService.cancelJob("99999", "admin"))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void cancelJob_shouldThrowWhenAlreadyCancelled() {
        sampleJob.setJobStatus(JobStatus.CANCELLED);
        when(scheduledJobRepository.findById("12345")).thenReturn(Optional.of(sampleJob));

        assertThatThrownBy(() -> schedulerService.cancelJob("12345", "admin"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void cancelJob_shouldThrowWhenAlreadyCompleted() {
        sampleJob.setJobStatus(JobStatus.COMPLETED);
        when(scheduledJobRepository.findById("12345")).thenReturn(Optional.of(sampleJob));

        assertThatThrownBy(() -> schedulerService.cancelJob("12345", "admin"))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void getJobsReadyForExecution_shouldReturnReadyJobs() {
        when(scheduledJobRepository.findJobsReadyForExecution(any(LocalDateTime.class)))
                .thenReturn(List.of(sampleJob));

        List<JobResponse> result = schedulerService.getJobsReadyForExecution();

        assertThat(result).hasSize(1);
    }

    @Test
    void getJobsForRetry_shouldReturnRetryableJobs() {
        when(scheduledJobRepository.findJobsForRetry()).thenReturn(List.of(sampleJob));

        List<JobResponse> result = schedulerService.getJobsForRetry();

        assertThat(result).hasSize(1);
    }

    @Test
    void cleanupOldJobs_shouldCallRepositoryDelete() {
        schedulerService.cleanupOldJobs(30);

        verify(scheduledJobRepository).deleteByJobStatusAndUpdatedAtBefore(
                eq(JobStatus.COMPLETED), any(LocalDateTime.class));
    }
}
