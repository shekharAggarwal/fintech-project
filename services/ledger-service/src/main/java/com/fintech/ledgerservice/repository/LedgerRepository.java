package com.fintech.ledgerservice.repository;

import com.fintech.ledgerservice.entity.LedgerEntry;
import com.fintech.ledgerservice.entity.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, String> {

    @Query("SELECT le FROM LedgerEntry le WHERE le.txnId = :txnId ORDER BY le.createdAt")
    List<LedgerEntry> findByTxnId(@Param("txnId") String txnId);

    List<LedgerEntry> findByAccountNumber(String accountNumber);

    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le WHERE le.txnId = :txnId AND le.entryType = :entryType")
    BigDecimal sumAmountByTxnIdAndEntryType(@Param("txnId") String txnId, @Param("entryType") LedgerEntryType entryType);

    @Query("SELECT COALESCE(SUM(le.amount), 0) FROM LedgerEntry le WHERE le.accountNumber = :accountNumber AND le.entryType = :entryType")
    BigDecimal sumAmountByAccountNumberAndEntryType(@Param("accountNumber") String accountNumber, @Param("entryType") LedgerEntryType entryType);

    @Query("SELECT le FROM LedgerEntry le WHERE le.accountNumber = :accountNumber AND le.createdAt BETWEEN :startDate AND :endDate ORDER BY le.createdAt DESC")
    List<LedgerEntry> findByAccountNumberAndCreatedAtBetween(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}

