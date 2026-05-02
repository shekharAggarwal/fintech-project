package com.fintech.ledgerservice.service;

import com.fintech.ledgerservice.dto.message.TransactionCompletedMessage;
import com.fintech.ledgerservice.dto.response.AccountBalanceResponse;
import com.fintech.ledgerservice.entity.LedgerEntry;
import com.fintech.ledgerservice.entity.LedgerEntryType;
import com.fintech.ledgerservice.repository.LedgerRepository;
import com.fintech.ledgerservice.util.SnowflakeIdGenerator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
public class LedgerService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerRepository ledgerRepo;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public LedgerService(LedgerRepository ledgerRepo,
                         SnowflakeIdGenerator snowflakeIdGenerator) {
        this.ledgerRepo = ledgerRepo;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * Creates a double-entry pair (DEBIT + CREDIT) for a completed transaction.
     * Guarantees atomicity via @Transactional — both entries commit or neither does.
     */
    @Transactional
    public void createDoubleEntry(String txnId, String senderAcct, String receiverAcct, BigDecimal amount, String paymentId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be positive");
        }
        if (senderAcct == null || senderAcct.isBlank()) {
            throw new IllegalArgumentException("Sender account must not be blank");
        }
        if (receiverAcct == null || receiverAcct.isBlank()) {
            throw new IllegalArgumentException("Receiver account must not be blank");
        }
        if (senderAcct.equals(receiverAcct)) {
            throw new IllegalArgumentException("Sender and receiver accounts must differ");
        }

        logger.info("Creating double entry for txnId: {}, sender: {}, receiver: {}, amount: {}",
                txnId, senderAcct, receiverAcct, amount);

        LedgerEntry debitEntry = new LedgerEntry(
                snowflakeIdGenerator.nextId(),
                txnId,
                paymentId,
                senderAcct,
                LedgerEntryType.DEBIT,
                amount,
                description
        );

        LedgerEntry creditEntry = new LedgerEntry(
                snowflakeIdGenerator.nextId(),
                txnId,
                paymentId,
                receiverAcct,
                LedgerEntryType.CREDIT,
                amount,
                description
        );

        ledgerRepo.save(debitEntry);
        ledgerRepo.save(creditEntry);

        logger.info("Successfully created double entry for txnId: {}", txnId);
    }

    /**
     * Creates reversal entries (opposite direction) for a failed transaction.
     * CREDIT on sender + DEBIT on receiver to undo the original entry.
     */
    @Transactional
    public void createReversalEntries(String txnId, String senderAcct, String receiverAcct, BigDecimal amount, String paymentId, String description) {
        logger.info("Creating reversal entries for txnId: {}, sender: {}, receiver: {}, amount: {}",
                txnId, senderAcct, receiverAcct, amount);

        String reversalDescription = "REVERSAL: " + (description != null ? description : "Transaction failed");

        LedgerEntry reversalCredit = new LedgerEntry(
                snowflakeIdGenerator.nextId(),
                txnId,
                paymentId,
                senderAcct,
                LedgerEntryType.CREDIT,
                amount,
                reversalDescription
        );

        LedgerEntry reversalDebit = new LedgerEntry(
                snowflakeIdGenerator.nextId(),
                txnId,
                paymentId,
                receiverAcct,
                LedgerEntryType.DEBIT,
                amount,
                reversalDescription
        );

        ledgerRepo.save(reversalCredit);
        ledgerRepo.save(reversalDebit);

        logger.info("Successfully created reversal entries for txnId: {}", txnId);
    }

    /**
     * Reconciles a transaction by verifying sum(DEBIT) == sum(CREDIT).
     * Returns true if balanced, false otherwise.
     */
    public boolean reconcile(String txnId) {
        logger.info("Reconciling transaction: {}", txnId);

        BigDecimal totalDebits = ledgerRepo.sumAmountByTxnIdAndEntryType(txnId, LedgerEntryType.DEBIT);
        BigDecimal totalCredits = ledgerRepo.sumAmountByTxnIdAndEntryType(txnId, LedgerEntryType.CREDIT);

        boolean balanced = totalDebits.compareTo(totalCredits) == 0;

        if (!balanced) {
            logger.warn("Transaction {} is NOT balanced. Debits: {}, Credits: {}", txnId, totalDebits, totalCredits);
        } else {
            logger.info("Transaction {} is balanced. Total: {}", txnId, totalDebits);
        }

        return balanced;
    }

    /**
     * Computes account balance as sum(CREDIT) - sum(DEBIT) for the given account.
     * Uses a single query to ensure read isolation (no phantom reads between two queries).
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public AccountBalanceResponse getAccountBalance(String accountId) {
        logger.info("Computing balance for account: {}", accountId);

        BigDecimal currentBalance = ledgerRepo.calculateNetBalance(accountId);

        return new AccountBalanceResponse(
                null,
                accountId,
                currentBalance,
                currentBalance,
                BigDecimal.ZERO,
                "USD"
        );
    }

    /**
     * Returns all ledger entries for an account within a date range (account statement).
     */
    public List<LedgerEntry> getAccountStatement(String accountId, Instant startDate, Instant endDate) {
        logger.info("Fetching statement for account: {} from {} to {}", accountId, startDate, endDate);
        return ledgerRepo.findByAccountNumberAndCreatedAtBetween(accountId, startDate, endDate);
    }

    /**
     * Returns all ledger entries for a specific transaction.
     */
    public List<LedgerEntry> getEntriesByTransactionId(String txnId) {
        return ledgerRepo.findByTxnId(txnId);
    }

    /**
     * Returns all ledger entries for a specific account.
     */
    public List<LedgerEntry> getEntriesByAccountId(String accountId) {
        return ledgerRepo.findByAccountNumber(accountId);
    }

    /**
     * Returns all ledger entries, paginated.
     */
    public Page<LedgerEntry> getAllEntries(Pageable pageable) {
        return ledgerRepo.findAll(pageable);
    }

    /**
     * Legacy method — creates ledger entry from TransactionCompletedMessage DTO.
     */
    @Transactional
    public void createLedgerEntry(TransactionCompletedMessage transactionCompletedMessage) {

        logger.info("Processing double entry for txnId: {}, debit: {}, credit: {}, amount: {}",
                transactionCompletedMessage.getTxnId(),
                transactionCompletedMessage.getFromAccount(),
                transactionCompletedMessage.getToAccount(),
                transactionCompletedMessage.getAmount());

        createDoubleEntry(
                transactionCompletedMessage.getTxnId(),
                transactionCompletedMessage.getFromAccount(),
                transactionCompletedMessage.getToAccount(),
                transactionCompletedMessage.getAmount(),
                transactionCompletedMessage.getPaymentId(),
                transactionCompletedMessage.getDescription()
        );

        logger.info("Successfully processed double entry for txnId: {}", transactionCompletedMessage.getTxnId());
    }
}
