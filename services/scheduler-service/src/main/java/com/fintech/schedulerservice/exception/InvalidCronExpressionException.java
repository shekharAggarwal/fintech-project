package com.fintech.schedulerservice.exception;

/**
 * Exception thrown when an invalid cron expression is provided
 */
public class InvalidCronExpressionException extends RuntimeException {

    private final String cronExpression;

    public InvalidCronExpressionException(String cronExpression) {
        super("Invalid cron expression: " + cronExpression);
        this.cronExpression = cronExpression;
    }

    public InvalidCronExpressionException(String cronExpression, Throwable cause) {
        super("Invalid cron expression: " + cronExpression, cause);
        this.cronExpression = cronExpression;
    }

    public String getCronExpression() {
        return cronExpression;
    }
}
