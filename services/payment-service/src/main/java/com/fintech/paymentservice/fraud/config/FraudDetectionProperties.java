package com.fintech.paymentservice.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "fraud.detection")
public class FraudDetectionProperties {

    private boolean enabled = true;
    private int approveThreshold = 30;
    private int blockThreshold = 70;

    private VelocityConfig velocity = new VelocityConfig();
    private AmountConfig amount = new AmountConfig();
    private TimeConfig time = new TimeConfig();
    private DestinationConfig destination = new DestinationConfig();
    private BlacklistConfig blacklist = new BlacklistConfig();

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getApproveThreshold() { return approveThreshold; }
    public void setApproveThreshold(int approveThreshold) { this.approveThreshold = approveThreshold; }

    public int getBlockThreshold() { return blockThreshold; }
    public void setBlockThreshold(int blockThreshold) { this.blockThreshold = blockThreshold; }

    public VelocityConfig getVelocity() { return velocity; }
    public void setVelocity(VelocityConfig velocity) { this.velocity = velocity; }

    public AmountConfig getAmount() { return amount; }
    public void setAmount(AmountConfig amount) { this.amount = amount; }

    public TimeConfig getTime() { return time; }
    public void setTime(TimeConfig time) { this.time = time; }

    public DestinationConfig getDestination() { return destination; }
    public void setDestination(DestinationConfig destination) { this.destination = destination; }

    public BlacklistConfig getBlacklist() { return blacklist; }
    public void setBlacklist(BlacklistConfig blacklist) { this.blacklist = blacklist; }

    public static class VelocityConfig {
        private int maxTransactions = 10;
        private int windowMinutes = 5;
        private int riskScore = 40;

        public int getMaxTransactions() { return maxTransactions; }
        public void setMaxTransactions(int maxTransactions) { this.maxTransactions = maxTransactions; }
        public int getWindowMinutes() { return windowMinutes; }
        public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    }

    public static class AmountConfig {
        private BigDecimal anomalyMultiplier = new BigDecimal("3.0");
        private int riskScore = 50;

        public BigDecimal getAnomalyMultiplier() { return anomalyMultiplier; }
        public void setAnomalyMultiplier(BigDecimal anomalyMultiplier) { this.anomalyMultiplier = anomalyMultiplier; }
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    }

    public static class TimeConfig {
        private int suspiciousStartHour = 1;
        private int suspiciousEndHour = 5;
        private int riskScore = 25;

        public int getSuspiciousStartHour() { return suspiciousStartHour; }
        public void setSuspiciousStartHour(int suspiciousStartHour) { this.suspiciousStartHour = suspiciousStartHour; }
        public int getSuspiciousEndHour() { return suspiciousEndHour; }
        public void setSuspiciousEndHour(int suspiciousEndHour) { this.suspiciousEndHour = suspiciousEndHour; }
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    }

    public static class DestinationConfig {
        private BigDecimal largeAmountThreshold = new BigDecimal("5000.0");
        private int riskScore = 35;

        public BigDecimal getLargeAmountThreshold() { return largeAmountThreshold; }
        public void setLargeAmountThreshold(BigDecimal largeAmountThreshold) { this.largeAmountThreshold = largeAmountThreshold; }
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    }

    public static class BlacklistConfig {
        private int riskScore = 90;
        private List<String> accounts = new ArrayList<>();

        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
        public List<String> getAccounts() { return accounts; }
        public void setAccounts(List<String> accounts) { this.accounts = accounts; }
    }
}
