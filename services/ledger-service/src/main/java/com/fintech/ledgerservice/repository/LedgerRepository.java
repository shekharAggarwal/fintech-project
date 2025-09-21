package com.fintech.ledgerservice.repository;

import com.fintech.ledgerservice.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, String> {
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.txnId = :txnId ORDER BY le.createdAt")
    List<LedgerEntry> findByTxnId(@Param("txnId") String txnId);

}

