package com.fintech.paymentservice.repository;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    
    // Since paymentId is now the primary key, we can use standard JPA methods
    // Optional<Payment> findByPaymentId(String paymentId); - Not needed anymore, use findById()
    
    Optional<Payment> findByUserId(String userId);

    List<Payment> findByStatusAndProcessingStartedAtBefore(PaymentStatus status, Instant before);

    List<Payment> findByStatus(PaymentStatus status);
    
    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);
    
    List<Payment> findByFromAccountOrToAccount(String fromAccount, String toAccount);
}