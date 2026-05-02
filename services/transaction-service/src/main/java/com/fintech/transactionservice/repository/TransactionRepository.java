package com.fintech.transactionservice.repository;

import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT t FROM Transaction t WHERE t.paymentId = ?1")
    Optional<Transaction> findByPaymentId(String paymentId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount = :accountId OR t.toAccount = :accountId")
    List<Transaction> findByAccountId(@Param("accountId") String accountId);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccount = :accountId OR t.toAccount = :accountId")
    Page<Transaction> findByAccountId(@Param("accountId") String accountId, Pageable pageable);

    List<Transaction> findByStatus(TransactionStatus status);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.fromAccount = :accountId OR t.toAccount = :accountId) AND t.status = :status")
    List<Transaction> findByAccountIdAndStatus(@Param("accountId") String accountId, @Param("status") TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE (t.fromAccount = :accountId OR t.toAccount = :accountId) AND t.status = :status")
    Page<Transaction> findByAccountIdAndStatus(@Param("accountId") String accountId, @Param("status") TransactionStatus status, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status")
    List<Transaction> findRetryableTransactions(@Param("status") TransactionStatus status);
}
