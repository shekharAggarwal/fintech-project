package com.fintech.transactionservice.dto.message;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public class AccountCreationMessage {
    @NotBlank(message = "User ID is required")
    private String userId;

    @DecimalMin(value = "0.0", message = "Initial deposit must be non-negative")
    private Double initialDeposit;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    // Default constructor
    public AccountCreationMessage() {
    }

    public AccountCreationMessage(String userId, Double initialDeposit, String accountNumber) {
        this.userId = userId;
        this.initialDeposit = initialDeposit;
        this.accountNumber = accountNumber;
    }


    public String getUserId() {
        return userId;
    }

    public Double getInitialDeposit() {
        return initialDeposit;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
