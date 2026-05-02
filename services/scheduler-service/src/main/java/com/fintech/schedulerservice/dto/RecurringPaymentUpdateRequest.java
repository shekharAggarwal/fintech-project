package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.entity.PaymentFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating a recurring payment
 */
public record RecurringPaymentUpdateRequest(

        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        PaymentFrequency frequency,

        String destinationAccountId,

        LocalDate endDate,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Min(value = 1, message = "Max retries must be at least 1")
        @Max(value = 10, message = "Max retries cannot exceed 10")
        Integer maxRetries
) {}
