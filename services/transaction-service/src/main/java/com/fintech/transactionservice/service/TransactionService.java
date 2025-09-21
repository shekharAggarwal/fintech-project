package com.fintech.transactionservice.service;

import com.fintech.transactionservice.adapter.BankAdapter;
import com.fintech.transactionservice.adapter.BankAdapterFactory;
import com.fintech.transactionservice.dto.message.PaymentInitiatedEvent;
import com.fintech.transactionservice.dto.message.TransactionCompletedEvent;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.entity.TransactionStatus;
import com.fintech.transactionservice.messaging.TransactionCompletedEventPublisher;
import com.fintech.transactionservice.model.TransactionResult;
import com.fintech.transactionservice.repository.TransactionRepository;
import com.fintech.transactionservice.util.SnowflakeIdGenerator;
import io.micrometer.observation.annotation.Observed;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Observed
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final TransactionCompletedEventPublisher transactionCompletedEventPublisher;
    private final BankAdapterFactory bankAdapterFactory;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            SnowflakeIdGenerator snowflakeIdGenerator,
            TransactionCompletedEventPublisher transactionCompletedEventPublisher,
            BankAdapterFactory bankAdapterFactory,
            TransactionRepository transactionRepository
    ) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.transactionCompletedEventPublisher = transactionCompletedEventPublisher;
        this.bankAdapterFactory = bankAdapterFactory;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction createTransaction(PaymentInitiatedEvent event) {
        logger.info("Creating transaction for paymentId: {}, payer account: {}, receiver account: {}, amount: {}, userId: {}",
                event.getPaymentId(), event.getFromAccount(), event.getToAccount(), event.getAmount(), event.getUserId());

        // 1. Idempotency Check
        Optional<Transaction> existing = transactionRepository.findByPaymentId(event.getPaymentId());
        if (existing.isPresent()) {
            logger.warn("Transaction already exists for paymentId: {}", event.getPaymentId());
            throw new IllegalStateException("Transaction already exists for paymentId: " + event.getPaymentId());
        }

        // 2. Create Transaction with PENDING status
        Transaction transaction = new Transaction(
                snowflakeIdGenerator.nextId(),
                event.getPaymentId(),
                event.getUserId(),
                event.getFromAccount(),
                event.getToAccount(),
                event.getAmount(),
                event.getDescription()
        );
        transaction.setStatus(TransactionStatus.PENDING);
        transaction = transactionRepository.save(transaction);

        logger.info("Created transaction: {} for paymentId: {}",
                transaction.getTxnId(), event.getPaymentId());

        try {
            // 3. Call BankAdapter for debit/credit execution
            BankAdapter adapter = bankAdapterFactory.getAdapter("Self");
            if (adapter == null) {
                throw new IllegalArgumentException("No adapter configured for bank: " + "Self");
            }

            TransactionResult result = adapter.process(transaction);

            if (result.success()) {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transactionRepository.save(transaction);

            } else {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
            }
        } catch (Exception ex) {
            logger.error("Transaction processing failed for txnId: {}", transaction.getTxnId(), ex);
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
        }

        // 4. Publish TransactionCompletedEvent for ledger and payment services to acknowledge
        TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(
                transaction.getTxnId(),
                transaction.getPaymentId(),
                transaction.getUserId(),
                transaction.getFromAccount(),
                transaction.getToAccount(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getStatus().name()
        );
        transactionCompletedEventPublisher.publishTransactionCompleted(completedEvent);
        logger.info("TransactionCompletedEvent published for txnId: {}", transaction.getTxnId());


        return transaction;
    }

/*
    @CircuitBreaker(name = "ledgerService", fallbackMethod = "fallbackProcessLedgerEntry")
    @Retry(name = "ledgerProcessing")
    @Transactional
    public Transaction processLedgerEntry(String transactionId) {
        logger.info("Processing ledger entry for transactionId: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.isProcessed()) {
            logger.info("Transaction already processed: {}", transactionId);
            return transaction;
        }

        try {
            transaction.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(transaction);

            // Call ledger service through external client
            LedgerEntryRequest ledgerRequest =
                    new LedgerEntryRequest(
                            transaction.getTransactionId(),
                            transaction.getPayerId(),
                            transaction.getReceiverId(),
                            transaction.getAmount(),
                            transaction.getCurrency(),
                            transaction.getDescription() != null ? transaction.getDescription() : "Payment transaction"
                    );

            logger.info("Calling ledger service for transactionId: {} with request: {}",
                    transactionId, ledgerRequest);

            LedgerEntryResponse response = ledgerServiceClient.createLedgerEntry(ledgerRequest);

            if (response != null && response.isSuccess()) {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setLedgerEntryId(response.getTransactionId());
                transaction.setProcessedAt(Instant.now());

                // Save transaction first
                Transaction savedTransaction = transactionRepository.save(transaction);

                // Publish transaction-recorded event
                publishTransactionRecordedEvent(savedTransaction);

                logger.info("Successfully processed ledger entry for transactionId: {}", transactionId);
                return savedTransaction;
            } else {
                String errorMessage = response != null ? response.getErrorMessage() : "Unknown error";
                throw new RuntimeException("Ledger service returned error: " + errorMessage);
            }

        } catch (Exception e) {
            logger.error("Failed to process ledger entry for transactionId: {} - {}",
                    transactionId, e.getMessage(), e);

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transaction.incrementRetryCount();

            if (transaction.canRetry()) {
                transaction.setStatus(TransactionStatus.RETRY_REQUIRED);
                logger.info("Transaction marked for retry: {} (attempt {})",
                        transactionId, transaction.getRetryCount());
            }
        }

        return transactionRepository.save(transaction);
    }

    public Transaction fallbackProcessLedgerEntry(String transactionId, Exception ex) {
        logger.error("Circuit breaker fallback for transactionId: {} - {}", transactionId, ex.getMessage());

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        transaction.setStatus(TransactionStatus.RETRY_REQUIRED);
        transaction.setFailureReason("Circuit breaker activated: " + ex.getMessage());
        transaction.incrementRetryCount();

        return transactionRepository.save(transaction);
    }

    public Optional<Transaction> findByPaymentId(String paymentId) {
        return transactionRepository.findByPaymentId(paymentId);
    }

    public List<Transaction> findRetryableTransactions() {
        return transactionRepository.findRetryableTransactions(TransactionStatus.RETRY_REQUIRED);
    }

    public TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(transaction.getTransactionId());
        response.setPaymentId(transaction.getPaymentId());
        response.setPayerId(transaction.getPayerId());
        response.setReceiverId(transaction.getReceiverId());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setStatus(transaction.getStatus());
        response.setLedgerEntryId(transaction.getLedgerEntryId());
        response.setDescription(transaction.getDescription());
        response.setFailureReason(transaction.getFailureReason());
        response.setRetryCount(transaction.getRetryCount());
        response.setProcessedAt(transaction.getProcessedAt());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }

    */
    /**
     * Publishes a transaction-recorded event when a transaction is successfully completed
     */
    /*
    private void publishTransactionRecordedEvent(Transaction transaction) {
        try {
            TransactionRecordedEvent event = TransactionRecordedEvent.builder()
                    .transactionId(Long.valueOf(transaction.getTransactionId()))
                    .paymentId(Long.valueOf(transaction.getPaymentId()))
                    .ledgerEntryId(transaction.getLedgerEntryId() != null ? Long.valueOf(transaction.getLedgerEntryId()) : null)
                    .userId(transaction.getPayerId())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .status(transaction.getStatus().name())
                    .description(transaction.getDescription())
                    .recordedAt(transaction.getCreatedAt() != null ?
                            LocalDateTime.ofInstant(transaction.getCreatedAt(), ZoneId.systemDefault()) : null)
                    .processedAt(transaction.getProcessedAt() != null ?
                            LocalDateTime.ofInstant(transaction.getProcessedAt(), ZoneId.systemDefault()) : null)
                    .build();

            eventPublisher.publishTransactionRecorded(event);

        } catch (Exception e) {
            logger.error("Failed to publish transaction-recorded event for transaction: {} - {}",
                    transaction.getTransactionId(), e.getMessage(), e);
        }
    }*/
}
