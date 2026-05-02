package com.fintech.transactionservice.repository;

import com.fintech.transactionservice.entity.Reversal;
import com.fintech.transactionservice.entity.ReversalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReversalRepository extends JpaRepository<Reversal, String> {

    List<Reversal> findByOriginalTransactionId(String originalTransactionId);

    List<Reversal> findByStatus(ReversalStatus status);
}
