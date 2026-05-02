package com.fintech.schedulerservice.entity;

/**
 * Enum representing payment frequency with associated cron expressions
 */
public enum PaymentFrequency {

    DAILY("0 0 6 * * ?"),           // Every day at 6 AM
    WEEKLY("0 0 6 ? * MON"),        // Every Monday at 6 AM
    BIWEEKLY("0 0 6 ? * MON/2"),    // Every other Monday at 6 AM
    MONTHLY("0 0 6 1 * ?"),         // First of every month at 6 AM
    QUARTERLY("0 0 6 1 1/3 ?"),     // First of every quarter at 6 AM
    YEARLY("0 0 6 1 1 ?");          // January 1st at 6 AM

    private final String cronExpression;

    PaymentFrequency(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getCronExpression() {
        return cronExpression;
    }
}
