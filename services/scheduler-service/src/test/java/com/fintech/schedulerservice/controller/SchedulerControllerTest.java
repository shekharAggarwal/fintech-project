package com.fintech.schedulerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.schedulerservice.dto.JobRequest;
import com.fintech.schedulerservice.dto.JobResponse;
import com.fintech.schedulerservice.dto.JobStatusUpdate;
import com.fintech.schedulerservice.entity.JobStatus;
import com.fintech.schedulerservice.entity.JobType;
import com.fintech.schedulerservice.exception.GlobalExceptionHandler;
import com.fintech.schedulerservice.exception.InvalidJobStateException;
import com.fintech.schedulerservice.exception.JobNotFoundException;
import com.fintech.schedulerservice.service.SchedulerService;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SchedulerController.class)
@AutoConfigureMockMvc(addFilters = false)
class SchedulerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchedulerService schedulerService;

    private ObjectMapper objectMapper;
    private JobResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = JobResponse.builder()
                .jobId("12345")
                .jobName("Test Job")
                .jobType(JobType.PAYMENT_RETRY)
                .jobStatus(JobStatus.SCHEDULED)
                .scheduledTime(Instant.now().plusSeconds(3600))
                .description("A test job")
                .createdBy("admin")
                .lastUpdatedBy("admin")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .retryCount(0)
                .maxRetries(3)
                .retryDelaySeconds(60)
                .priority("NORMAL")
                .build();
    }

    @Test
    void createJob_shouldReturn201() throws Exception {
        JobRequest request = new JobRequest(
                "Test Job", JobType.PAYMENT_RETRY, LocalDateTime.now().plusHours(1),
                "A test job", "admin", null, 3, 60, "NORMAL"
        );

        when(schedulerService.createJob(any(JobRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value("12345"))
                .andExpect(jsonPath("$.jobName").value("Test Job"))
                .andExpect(jsonPath("$.jobStatus").value("SCHEDULED"));
    }

    @Test
    void getJobById_shouldReturn200WhenExists() throws Exception {
        when(schedulerService.getJobById("12345")).thenReturn(Optional.of(sampleResponse));

        mockMvc.perform(get("/api/v1/scheduler/jobs/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("12345"))
                .andExpect(jsonPath("$.jobName").value("Test Job"));
    }

    @Test
    void getJobById_shouldReturn404WhenNotExists() throws Exception {
        when(schedulerService.getJobById("99999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/scheduler/jobs/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJobs_shouldReturn200WithStatusFilter() throws Exception {
        Page<JobResponse> page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1);
        when(schedulerService.getJobsByStatus(eq(JobStatus.SCHEDULED), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/scheduler/jobs")
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobId").value("12345"));
    }

    @Test
    void getJobs_shouldReturn200WithoutStatusFilter() throws Exception {
        Page<JobResponse> page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1);
        when(schedulerService.getAllJobs(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/scheduler/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobId").value("12345"));
    }

    @Test
    void getJobsByType_shouldReturn200() throws Exception {
        when(schedulerService.getJobsByType(JobType.PAYMENT_RETRY)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/scheduler/jobs/by-type/PAYMENT_RETRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value("12345"))
                .andExpect(jsonPath("$[0].jobType").value("PAYMENT_RETRY"));
    }

    @Test
    void updateJobStatus_shouldReturn200() throws Exception {
        JobStatusUpdate update = new JobStatusUpdate(
                "12345", JobStatus.COMPLETED, "Success", null, null, "admin"
        );

        when(schedulerService.updateJobStatus(any(JobStatusUpdate.class))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/scheduler/jobs/12345/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("12345"));
    }

    @Test
    void cancelJob_shouldReturn204() throws Exception {
        when(schedulerService.cancelJob("12345", "admin")).thenReturn(sampleResponse);

        mockMvc.perform(delete("/api/v1/scheduler/jobs/12345")
                        .param("updatedBy", "admin"))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelJob_shouldReturn404WhenNotFound() throws Exception {
        when(schedulerService.cancelJob("99999", "admin"))
                .thenThrow(new JobNotFoundException("99999"));

        mockMvc.perform(delete("/api/v1/scheduler/jobs/99999")
                        .param("updatedBy", "admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelJob_shouldReturn409WhenInvalidState() throws Exception {
        when(schedulerService.cancelJob("12345", "admin"))
                .thenThrow(new InvalidJobStateException("12345", JobStatus.COMPLETED, "cancel"));

        mockMvc.perform(delete("/api/v1/scheduler/jobs/12345")
                        .param("updatedBy", "admin"))
                .andExpect(status().isConflict());
    }

    @Test
    void getJobsReadyForExecution_shouldReturn200() throws Exception {
        when(schedulerService.getJobsReadyForExecution()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/scheduler/jobs/ready-for-execution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value("12345"));
    }

    @Test
    void getJobsForRetry_shouldReturn200() throws Exception {
        when(schedulerService.getJobsForRetry()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/scheduler/jobs/for-retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value("12345"));
    }

    @Test
    void cleanupOldJobs_shouldReturn204() throws Exception {
        doNothing().when(schedulerService).cleanupOldJobs(30);

        mockMvc.perform(delete("/api/v1/scheduler/jobs/cleanup")
                        .param("daysOld", "30"))
                .andExpect(status().isNoContent());
    }

    @Test
    void cleanupOldJobs_withDefaultDays_shouldReturn204() throws Exception {
        doNothing().when(schedulerService).cleanupOldJobs(30);

        mockMvc.perform(delete("/api/v1/scheduler/jobs/cleanup"))
                .andExpect(status().isNoContent());
    }
}
