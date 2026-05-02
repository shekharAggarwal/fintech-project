package com.fintech.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "transaction.limits")
public class TransactionLimitProperties {

    private BigDecimal perTransaction = new BigDecimal("10000.00");
    private BigDecimal daily = new BigDecimal("50000.00");
    private BigDecimal weekly = new BigDecimal("200000.00");
    private BigDecimal monthly = new BigDecimal("500000.00");
    private String defaultCurrency = "USD";
    private boolean autoCreateOnFirstTransaction = true;

    public BigDecimal getPerTransaction() { return perTransaction; }
    public void setPerTransaction(BigDecimal perTransaction) { this.perTransaction = perTransaction; }

    public BigDecimal getDaily() { return daily; }
    public void setDaily(BigDecimal daily) { this.daily = daily; }

    public BigDecimal getWeekly() { return weekly; }
    public void setWeekly(BigDecimal weekly) { this.weekly = weekly; }

    public BigDecimal getMonthly() { return monthly; }
    public void setMonthly(BigDecimal monthly) { this.monthly = monthly; }

    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }

    public boolean isAutoCreateOnFirstTransaction() { return autoCreateOnFirstTransaction; }
    public void setAutoCreateOnFirstTransaction(boolean autoCreateOnFirstTransaction) { this.autoCreateOnFirstTransaction = autoCreateOnFirstTransaction; }
}
