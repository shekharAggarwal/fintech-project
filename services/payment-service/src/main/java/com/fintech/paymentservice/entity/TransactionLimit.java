package com.fintech.paymentservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_limits", indexes = {
    @Index(name = "idx_txn_limit_account", columnList = "account_id"),
    @Index(name = "idx_txn_limit_account_type", columnList = "account_id, limit_type", unique = true)
})
public class TransactionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false)
    private LimitType limitType;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "current_usage", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentUsage = BigDecimal.ZERO;

    @Column(name = "reset_at")
    private Instant resetAt;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TransactionLimit() {}

    public TransactionLimit(String accountId, LimitType limitType, BigDecimal maxAmount, String currency) {
        this.accountId = accountId;
        this.limitType = limitType;
        this.maxAmount = maxAmount;
        this.currency = currency;
        this.currentUsage = BigDecimal.ZERO;
        this.enabled = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public LimitType getLimitType() { return limitType; }
    public void setLimitType(LimitType limitType) { this.limitType = limitType; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public BigDecimal getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(BigDecimal currentUsage) { this.currentUsage = currentUsage; }

    public Instant getResetAt() { return resetAt; }
    public void setResetAt(Instant resetAt) { this.resetAt = resetAt; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
