package com.fintech.transactionservice.entity;

import com.fintech.security.annotation.FieldAccessControl;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @FieldAccessControl(resourceType = "transaction", fieldName = "userId")
    private String userId;

    @Column(nullable = false, unique = true)
    @FieldAccessControl(resourceType = "transaction", fieldName = "accountNumber")
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    @FieldAccessControl(resourceType = "transaction", fieldName = "balance")
    private BigDecimal balance;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @FieldAccessControl(resourceType = "transaction", fieldName = "createdAt")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @FieldAccessControl(resourceType = "transaction", fieldName = "updatedAt")
    private Instant updatedAt;


    public Account(String userId, String accountNumber, BigDecimal balance) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public Account() {

    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
