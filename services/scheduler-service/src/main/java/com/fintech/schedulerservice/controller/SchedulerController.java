package com.fintech.schedulerservice.controller;

import com.fintech.schedulerservice.dto.JobRequest;
import com.fintech.schedulerservice.dto.JobResponse;
import com.fintech.schedulerservice.dto.JobStatusUpdate;
import com.fintech.schedulerservice.entity.JobStatus;
import com.fintech.schedulerservice.entity.JobType;
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
        //log.info("Creating new job: {}", jobRequest.getJobName());
        JobResponse response = schedulerService.createJob(jobRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get job by ID
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable String jobId) {
//        log.info("Getting job by ID: {}", jobId);
        return schedulerService.getJobById(jobId)
                .map(job -> ResponseEntity.ok(job))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get jobs by status with pagination
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<JobResponse>> getJobsByStatus(
            @RequestParam(required = false) JobStatus status,
            Pageable pageable) {
        //log.info("Getting jobs by status: {} with pagination", status);

        if (status != null) {
            Page<JobResponse> jobs = schedulerService.getJobsByStatus(status, pageable);
            return ResponseEntity.ok(jobs);
        }

        // If no status provided, return all jobs (implement in service if needed)
        return ResponseEntity.badRequest().build();
    }

    /**
     * Get jobs by type
     */
    @GetMapping("/jobs/by-type/{jobType}")
    public ResponseEntity<List<JobResponse>> getJobsByType(@PathVariable JobType jobType) {
        //log.info("Getting jobs by type: {}", jobType);
        List<JobResponse> jobs = schedulerService.getJobsByType(jobType);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Update job status
     */
    @PutMapping("/jobs/{jobId}/status")
    public ResponseEntity<JobResponse> updateJobStatus(
            @PathVariable String jobId,
            @Valid @RequestBody JobStatusUpdate statusUpdate) {
        //log.info("Updating job status for ID: {} to {}", jobId, statusUpdate.getJobStatus());

        statusUpdate.setJobId(jobId);
        JobResponse response = schedulerService.updateJobStatus(statusUpdate);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a scheduled job
     */
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<JobResponse> cancelJob(
            @PathVariable String jobId,
            @RequestParam String updatedBy) {
        //log.info("Cancelling job: {} by user: {}", jobId, updatedBy);
        JobResponse response = schedulerService.cancelJob(jobId, updatedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Get jobs ready for execution
     */
    @GetMapping("/jobs/ready-for-execution")
    public ResponseEntity<List<JobResponse>> getJobsReadyForExecution() {
        //log.info("Getting jobs ready for execution");
        List<JobResponse> jobs = schedulerService.getJobsReadyForExecution();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get jobs for retry
     */
    @GetMapping("/jobs/for-retry")
    public ResponseEntity<List<JobResponse>> getJobsForRetry() {
        //  log.info("Getting jobs for retry");
        List<JobResponse> jobs = schedulerService.getJobsForRetry();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Trigger cleanup of old jobs
     */
    @DeleteMapping("/jobs/cleanup")
    public ResponseEntity<Void> cleanupOldJobs(@RequestParam(defaultValue = "30") int daysOld) {
        //   log.info("Triggering cleanup of jobs older than {} days", daysOld);
        schedulerService.cleanupOldJobs(daysOld);
        return ResponseEntity.ok().build();
    }
}