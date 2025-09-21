package com.fintech.reportingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for transaction records used in reporting
 */
@Entity
@Table(name = "transaction_records", indexes = {
    @Index(name = "idx_payment_id", columnList = "payment_id"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_status_date", columnList = "status, transaction_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {

    @Id
    @Column(name = "record_id", length = 50)
    private String recordId;

    @Column(name = "transaction_id", nullable = false, length = 50)
    private String transactionId;

    @Column(name = "payment_id", length = 50)
    private String paymentId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "transaction_type", length = 50)
    private String transactionType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "merchant_id", length = 50)
    private String merchantId;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @ElementCollection
    @CollectionTable(name = "transaction_metadata", joinColumns = @JoinColumn(name = "record_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value", length = 1000)
    private Map<String, String> metadata;

    @Column(name = "fee_amount", precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}