package com.fintech.paymentservice.repository;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Additional methods for new APIs
    Page<Payment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND " +
           "(p.status = 'PENDING' OR p.status = 'PENDING_VERIFICATION' OR p.status = 'AUTHORIZED')")
    List<Payment> findCancellablePaymentsByUserId(@Param("userId") String userId);
    
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId AND p.userId = :userId AND " +
           "(p.status = 'PENDING' OR p.status = 'PENDING_VERIFICATION' OR p.status = 'AUTHORIZED')")
    Optional<Payment> findCancellablePayment(@Param("paymentId") String paymentId, @Param("userId") String userId);
}