package com.fintech.ledgerservice.entity;

import com.fintech.security.annotation.FieldAccessControl;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_txn_id", columnList = "txn_id"),
        @Index(name = "idx_ledger_account_id", columnList = "account_id"),
        @Index(name = "idx_ledger_created_at", columnList = "created_at"),
        @Index(name = "idx_ledger_entry_type", columnList = "entry_type")
})
public class LedgerEntry {

    @Id
    @Column(name = "entry_id", nullable = false, length = 20)
    @FieldAccessControl(resourceType = "ledger", fieldName = "entryId")
    private String entryId;

    @Column(name = "txn_id", nullable = false, length = 20)
    @FieldAccessControl(resourceType = "ledger", fieldName = "txnId")
    private String txnId;

    @Column(name = "payment_id", nullable = false, length = 50)
    @FieldAccessControl(resourceType = "ledger", fieldName = "paymentId", sensitive = true)
    private String paymentId;

    @Column(name = "account_number", nullable = false, length = 50)
    @FieldAccessControl(resourceType = "ledger", fieldName = "accountNumber")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    @FieldAccessControl(resourceType = "ledger", fieldName = "entryType")
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    @FieldAccessControl(resourceType = "ledger", fieldName = "amount")
    private BigDecimal amount;

    @Column(length = 500)
    @FieldAccessControl(resourceType = "ledger", fieldName = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @FieldAccessControl(resourceType = "ledger", fieldName = "createdAt")
    private Instant createdAt;


    // Constructors
    public LedgerEntry() {
    }

    public LedgerEntry(String entryId, String txnId, String paymentId, String accountNumber, LedgerEntryType entryType, BigDecimal amount, String description) {
        this.entryId = entryId;
        this.txnId = txnId;
        this.paymentId = paymentId;
        this.accountNumber = accountNumber;
        this.entryType = entryType;
        this.amount = amount;
        this.description = description;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(LedgerEntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}