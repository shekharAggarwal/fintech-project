package com.fintech.reportingservice.repository;

import com.fintech.reportingservice.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, UUID> {
}
