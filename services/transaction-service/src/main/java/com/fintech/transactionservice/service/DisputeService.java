package com.fintech.transactionservice.service;

import com.fintech.transactionservice.dto.request.DisputeRequest;
import com.fintech.transactionservice.dto.request.ReversalRequest;
import com.fintech.transactionservice.entity.*;
import com.fintech.transactionservice.exception.InvalidTransactionException;
import com.fintech.transactionservice.exception.TransactionNotFoundException;
import com.fintech.transactionservice.messaging.DisputeEventPublisher;
import com.fintech.transactionservice.repository.DisputeRepository;
import com.fintech.transactionservice.repository.ReversalRepository;
import com.fintech.transactionservice.repository.TransactionRepository;
import com.fintech.transactionservice.util.SnowflakeIdGenerator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DisputeService {

    private static final Logger logger = LoggerFactory.getLogger(DisputeService.class);

    private final DisputeRepository disputeRepository;
    private final ReversalRepository reversalRepository;
    private final TransactionRepository transactionRepository;
    private final DisputeEventPublisher disputeEventPublisher;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public DisputeService(DisputeRepository disputeRepository,
                          ReversalRepository reversalRepository,
                          TransactionRepository transactionRepository,
                          DisputeEventPublisher disputeEventPublisher,
                          SnowflakeIdGenerator snowflakeIdGenerator) {
        this.disputeRepository = disputeRepository;
        this.reversalRepository = reversalRepository;
        this.transactionRepository = transactionRepository;
        this.disputeEventPublisher = disputeEventPublisher;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Transactional
    public Dispute openDispute(DisputeRequest request) {
        // Verify transaction exists
        transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException(request.getTransactionId()));

        String disputeId = UUID.randomUUID().toString();
        Dispute dispute = new Dispute(disputeId, request.getTransactionId(), request.getDisputeType(), request.getReason());
        dispute.setEvidence(request.getEvidence());
        dispute.setRefundAmount(request.getRefundAmount());

        dispute = disputeRepository.save(dispute);
        logger.info("Dispute opened: {} for transaction: {}", disputeId, request.getTransactionId());

        disputeEventPublisher.publishDisputeOpened(disputeId, request.getTransactionId(), request.getDisputeType().name());
        return dispute;
    }

    @Transactional
    public Dispute reviewDispute(String disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new InvalidTransactionException("Dispute not found: " + disputeId));

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new InvalidTransactionException("Dispute cannot be reviewed in current status: " + dispute.getStatus());
        }

        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        dispute = disputeRepository.save(dispute);
        logger.info("Dispute moved to UNDER_REVIEW: {}", disputeId);
        return dispute;
    }

    @Transactional
    public Dispute resolveDispute(String disputeId, String resolution, DisputeStatus resolvedStatus) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new InvalidTransactionException("Dispute not found: " + disputeId));

        if (dispute.getStatus() != DisputeStatus.UNDER_REVIEW && dispute.getStatus() != DisputeStatus.OPEN) {
            throw new InvalidTransactionException("Dispute cannot be resolved in current status: " + dispute.getStatus());
        }

        // Validate resolved status
        if (resolvedStatus != DisputeStatus.RESOLVED_APPROVED &&
                resolvedStatus != DisputeStatus.RESOLVED_DENIED &&
                resolvedStatus != DisputeStatus.RESOLVED_PARTIAL) {
            throw new InvalidTransactionException("Invalid resolution status: " + resolvedStatus);
        }

        dispute.setStatus(resolvedStatus);
        dispute.setResolution(resolution);
        dispute.setResolvedAt(Instant.now());
        dispute = disputeRepository.save(dispute);
        logger.info("Dispute resolved: {} with status: {}", disputeId, resolvedStatus);

        disputeEventPublisher.publishDisputeResolved(disputeId, dispute.getTransactionId(), resolvedStatus.name());
        return dispute;
    }

    @Transactional
    public Dispute cancelDispute(String disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new InvalidTransactionException("Dispute not found: " + disputeId));

        if (dispute.getStatus() == DisputeStatus.RESOLVED_APPROVED ||
                dispute.getStatus() == DisputeStatus.RESOLVED_DENIED ||
                dispute.getStatus() == DisputeStatus.RESOLVED_PARTIAL) {
            throw new InvalidTransactionException("Cannot cancel a resolved dispute");
        }

        dispute.setStatus(DisputeStatus.CANCELLED);
        dispute = disputeRepository.save(dispute);
        logger.info("Dispute cancelled: {}", disputeId);

        disputeEventPublisher.publishDisputeCancelled(disputeId, dispute.getTransactionId());
        return dispute;
    }

    public Dispute getDispute(String disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new InvalidTransactionException("Dispute not found: " + disputeId));
    }

    public List<Dispute> getDisputesByTransaction(String transactionId) {
        return disputeRepository.findByTransactionId(transactionId);
    }

    @Transactional
    public Reversal initiateReversal(ReversalRequest request) {
        // Verify original transaction exists and is completed
        Transaction originalTxn = transactionRepository.findById(request.getOriginalTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException(request.getOriginalTransactionId()));

        if (originalTxn.getStatus() != TransactionStatus.COMPLETED) {
            throw new InvalidTransactionException("Can only reverse COMPLETED transactions, current status: " + originalTxn.getStatus());
        }

        // Validate amount for partial refund
        if (request.getReversalType() == ReversalType.PARTIAL_REFUND &&
                request.getAmount().compareTo(originalTxn.getAmount()) > 0) {
            throw new InvalidTransactionException("Reversal amount cannot exceed original transaction amount");
        }

        String reversalId = UUID.randomUUID().toString();
        Reversal reversal = new Reversal(
                reversalId,
                request.getOriginalTransactionId(),
                request.getReversalType(),
                request.getAmount(),
                request.getReason()
        );

        // Create reversal transaction (reverse direction)
        String reversalTxnId = snowflakeIdGenerator.nextId();
        Transaction reversalTxn = new Transaction(
                reversalTxnId,
                reversalTxnId,
                originalTxn.getUserId(),
                originalTxn.getToAccount(),   // reversed direction
                originalTxn.getFromAccount(), // reversed direction
                request.getAmount(),
                "Reversal of transaction: " + request.getOriginalTransactionId()
        );
        reversalTxn.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(reversalTxn);

        reversal.setReversalTransactionId(reversalTxnId);
        reversal.setStatus(ReversalStatus.COMPLETED);
        reversal.setCompletedAt(Instant.now());
        reversal = reversalRepository.save(reversal);

        logger.info("Reversal completed: {} for original transaction: {}", reversalId, request.getOriginalTransactionId());

        disputeEventPublisher.publishReversalCompleted(reversalId, request.getOriginalTransactionId(), reversalTxnId);
        return reversal;
    }

    public Reversal getReversal(String reversalId) {
        return reversalRepository.findById(reversalId)
                .orElseThrow(() -> new InvalidTransactionException("Reversal not found: " + reversalId));
    }

    public List<Reversal> getReversalsByTransaction(String transactionId) {
        return reversalRepository.findByOriginalTransactionId(transactionId);
    }
}
