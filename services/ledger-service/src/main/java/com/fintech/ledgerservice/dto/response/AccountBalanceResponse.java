package com.fintech.ledgerservice.dto.response;

import java.math.BigDecimal;

public class AccountBalanceResponse {
    
    private String balanceId;
    private String accountId;
    private BigDecimal currentBalance;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private String currency;

    // Constructors
    public AccountBalanceResponse() {}

    public AccountBalanceResponse(String balanceId, String accountId, BigDecimal currentBalance, 
                                 BigDecimal availableBalance, BigDecimal pendingBalance, String currency) {
        this.balanceId = balanceId;
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.pendingBalance = pendingBalance;
        this.currency = currency;
    }

    // Getters and Setters
    public String getBalanceId() {
        return balanceId;
    }

    public void setBalanceId(String balanceId) {
        this.balanceId = balanceId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getPendingBalance() {
        return pendingBalance;
    }

    public void setPendingBalance(BigDecimal pendingBalance) {
        this.pendingBalance = pendingBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}