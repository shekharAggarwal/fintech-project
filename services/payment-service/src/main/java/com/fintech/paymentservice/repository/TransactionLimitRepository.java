package com.fintech.paymentservice.repository;

import com.fintech.paymentservice.entity.LimitType;
import com.fintech.paymentservice.entity.TransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {

    List<TransactionLimit> findByAccountId(String accountId);

    Optional<TransactionLimit> findByAccountIdAndLimitType(String accountId, LimitType limitType);

    List<TransactionLimit> findByAccountIdAndEnabled(String accountId, boolean enabled);

    @Modifying
    @Query("UPDATE TransactionLimit t SET t.currentUsage = 0, t.resetAt = :resetAt WHERE t.limitType = :limitType AND t.resetAt < :now")
    int resetLimitsByType(@Param("limitType") LimitType limitType, @Param("now") Instant now, @Param("resetAt") Instant resetAt);
}
