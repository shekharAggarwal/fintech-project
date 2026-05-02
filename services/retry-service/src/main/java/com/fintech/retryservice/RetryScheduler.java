package com.fintech.retryservice;


import com.fintech.retryservice.service.RetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RetryScheduler.class);

    private final RetryService retryService;

    public RetryScheduler(RetryService retryService) {
        this.retryService = retryService;
    }

    /**
     * Execute due retries every minute.
     */
    @Scheduled(fixedDelayString = "${retry.scan.ms:60000}")
    public void scan() {
        try {
            logger.debug("Retry scheduler scanning for due retries...");
            retryService.executeRetries();
        } catch (Exception ex) {
            logger.error("Error during retry execution scan: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Reset stuck retries every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${retry.stuck.scan.ms:300000}")
    public void resetStuck() {
        try {
            retryService.resetStuckRetries();
        } catch (Exception ex) {
            logger.error("Error resetting stuck retries: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Cleanup old records daily at 2 AM.
     */
    @Scheduled(cron = "${retry.cleanup.cron:0 0 2 * * ?}")
    public void cleanup() {
        try {
            retryService.cleanupOldRecords();
        } catch (Exception ex) {
            logger.error("Error cleaning up old retry records: {}", ex.getMessage(), ex);
        }
    }
}