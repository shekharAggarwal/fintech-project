package com.fintech.schedulerservice.job;

import com.fintech.schedulerservice.service.RecurringPaymentService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Quartz Job implementation for executing recurring payments
 */
@Component
public class RecurringPaymentJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(RecurringPaymentJob.class);

    private final RecurringPaymentService recurringPaymentService;

    public RecurringPaymentJob(RecurringPaymentService recurringPaymentService) {
        this.recurringPaymentService = recurringPaymentService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String paymentId = context.getJobDetail().getJobDataMap().getString("paymentId");
        log.info("Executing recurring payment job for payment: {}", paymentId);

        try {
            recurringPaymentService.executePayment(paymentId);
            log.info("Recurring payment executed successfully: {}", paymentId);
        } catch (Exception e) {
            log.error("Failed to execute recurring payment: {}", paymentId, e);
            throw new JobExecutionException("Recurring payment execution failed for: " + paymentId, e);
        }
    }
}
