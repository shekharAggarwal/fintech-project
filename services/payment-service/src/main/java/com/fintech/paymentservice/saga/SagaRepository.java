package com.fintech.paymentservice.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SagaRepository extends JpaRepository<SagaState, Long> {

    Optional<SagaState> findByPaymentId(String paymentId);

    List<SagaState> findByStatus(SagaStatus status);

    @Query("SELECT s FROM SagaState s WHERE s.status = :status AND s.startedAt < :timeout")
    List<SagaState> findStuckSagas(@Param("status") SagaStatus status, @Param("timeout") Instant timeout);
}
