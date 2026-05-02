package com.fintech.transactionservice.repository;

import com.fintech.transactionservice.entity.Dispute;
import com.fintech.transactionservice.entity.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, String> {

    List<Dispute> findByTransactionId(String transactionId);

    List<Dispute> findByStatus(DisputeStatus status);

    List<Dispute> findByTransactionIdAndStatus(String transactionId, DisputeStatus status);
}
