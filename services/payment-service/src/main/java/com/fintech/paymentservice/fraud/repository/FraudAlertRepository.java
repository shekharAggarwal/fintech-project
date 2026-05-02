package com.fintech.paymentservice.fraud.repository;

import com.fintech.paymentservice.fraud.entity.FraudAlert;
import com.fintech.paymentservice.fraud.model.AlertStatus;
import com.fintech.paymentservice.fraud.model.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByPaymentId(String paymentId);

    List<FraudAlert> findByAccountId(String accountId);

    Page<FraudAlert> findByStatus(AlertStatus status, Pageable pageable);

    Page<FraudAlert> findByAlertType(AlertType alertType, Pageable pageable);

    long countByAccountIdAndCreatedAtAfter(String accountId, Instant after);

    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
