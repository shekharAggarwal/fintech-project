package com.fintech.schedulerservice.controller;

import com.fintech.schedulerservice.dto.JobRequest;
import com.fintech.schedulerservice.dto.JobResponse;
import com.fintech.schedulerservice.dto.JobStatusUpdate;
import com.fintech.schedulerservice.entity.JobStatus;
import com.fintech.schedulerservice.entity.JobType;
import com.fintech.schedulerservice.exception.JobNotFoundException;
import com.fintech.schedulerservice.service.SchedulerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for scheduler service endpoints
 */
@RestController
@RequestMapping("/api/v1/scheduler")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    /**
     * Create a new scheduled job
     */
    @PostMapping("/jobs")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest jobRequest) {
        JobResponse response = schedulerService.createJob(jobRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get job by ID
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable String jobId) {
        return schedulerService.getJobById(jobId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    /**
     * Get jobs with optional status filter and pagination
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<JobResponse>> getJobs(
            @RequestParam(required = false) JobStatus status,
            Pageable pageable) {

        if (status != null) {
            Page<JobResponse> jobs = schedulerService.getJobsByStatus(status, pageable);
            return ResponseEntity.ok(jobs);
        }

        // Return all jobs when no status filter is provided
        Page<JobResponse> jobs = schedulerService.getAllJobs(pageable);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get jobs by type
     */
    @GetMapping("/jobs/by-type/{jobType}")
    public ResponseEntity<List<JobResponse>> getJobsByType(@PathVariable JobType jobType) {
        List<JobResponse> jobs = schedulerService.getJobsByType(jobType);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Update job status (partial update)
     */
    @PatchMapping("/jobs/{jobId}/status")
    public ResponseEntity<JobResponse> updateJobStatus(
            @PathVariable String jobId,
            @Valid @RequestBody JobStatusUpdate statusUpdate) {
        statusUpdate.setJobId(jobId);
        JobResponse response = schedulerService.updateJobStatus(statusUpdate);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a scheduled job
     */
    @DeleteMapping("/jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> cancelJob(
            @PathVariable String jobId,
            @RequestParam String updatedBy) {
        schedulerService.cancelJob(jobId, updatedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get jobs ready for execution
     */
    @GetMapping("/jobs/ready-for-execution")
    public ResponseEntity<List<JobResponse>> getJobsReadyForExecution() {
        List<JobResponse> jobs = schedulerService.getJobsReadyForExecution();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get jobs for retry
     */
    @GetMapping("/jobs/for-retry")
    public ResponseEntity<List<JobResponse>> getJobsForRetry() {
        List<JobResponse> jobs = schedulerService.getJobsForRetry();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Trigger cleanup of old jobs
     */
    @DeleteMapping("/jobs/cleanup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> cleanupOldJobs(@RequestParam(defaultValue = "30") int daysOld) {
        schedulerService.cleanupOldJobs(daysOld);
        return ResponseEntity.noContent().build();
    }
}