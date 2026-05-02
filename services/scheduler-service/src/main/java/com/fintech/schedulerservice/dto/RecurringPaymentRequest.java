package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.entity.PaymentFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a recurring payment
 */
public record RecurringPaymentRequest(

        @NotBlank(message = "User ID is required")
        String userId,

        @NotBlank(message = "Source account ID is required")
        String sourceAccountId,

        @NotBlank(message = "Destination account ID is required")
        String destinationAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "Frequency is required")
        PaymentFrequency frequency,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Min(value = 1, message = "Max retries must be at least 1")
        @Max(value = 10, message = "Max retries cannot exceed 10")
        Integer maxRetries
) {}
