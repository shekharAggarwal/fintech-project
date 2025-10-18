package com.fintech.paymentservice.service;

import com.fintech.paymentservice.dto.message.OtpNotificationEvent;
import com.fintech.paymentservice.dto.message.TransactionCompletedMessage;
import com.fintech.paymentservice.dto.request.InitiateRequest;
import com.fintech.paymentservice.dto.response.PaymentInitiatedResponse;
import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.messaging.OtpEmailPublisher;
import com.fintech.paymentservice.messaging.TransactionPublisher;
import com.fintech.paymentservice.model.PaymentStatus;
import com.fintech.paymentservice.repository.PaymentRepository;
import com.fintech.paymentservice.util.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    private final StringRedisTemplate redis;


    private final OtpEmailPublisher otpEmailPublisher;

    private final OtpService otpService;

    private final SnowflakeIdGenerator idGenerator;

    public PaymentService(PaymentRepository paymentRepository, StringRedisTemplate redis, TransactionPublisher eventPublisher, OtpEmailPublisher otpEmailPublisher, OtpService otpService, SnowflakeIdGenerator idGenerator) {
        this.paymentRepository = paymentRepository;
        this.redis = redis;
        this.otpEmailPublisher = otpEmailPublisher;
        this.otpService = otpService;
        this.idGenerator = idGenerator;
    }

    /**
     * Initiate a new payment with auto-generated payment ID
     */
    @Transactional
    public PaymentInitiatedResponse initiate(InitiateRequest request, String currentUserId) {
        logger.info("Initiating payment for user {} from {} to {} amount {}", currentUserId, request.fromAccount(), request.toAccount(), request.amount());

        // Generate unique payment ID using Snowflake
        String paymentId = idGenerator.generateStringId();

        // Create payment entity
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setUserId(currentUserId);
        payment.setFromAccount(request.fromAccount());
        payment.setToAccount(request.toAccount());
        payment.setAmount(request.amount());
        payment.setDescription(request.description());
        payment.setStatus(PaymentStatus.PENDING_VERIFICATION);
        payment.setCreatedAt(Instant.now());
        payment.setRetryCount(0);

        payment = paymentRepository.save(payment);

        // Generate OTP for verification
        String otp = otpService.generateOtp(paymentId);

        /// send otp to useId with amount
        try {
            otpEmailPublisher.publishOtpEmail(new OtpNotificationEvent(currentUserId, request.amount().toString(), otp));
            logger.info("OTP sent to user {} for paymentId: {}", currentUserId, paymentId);
        } catch (Exception e) {
            logger.error("Failed to send OTP notification for paymentId:{} to userId:{}", paymentId, currentUserId, e);
        }

        logger.info("Payment initiated successfully with ID: {}", paymentId);

        return new PaymentInitiatedResponse(payment.getPaymentId(),
                payment.getFromAccount(),
                payment.getToAccount(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getCreatedAt(),
                "Payment initiated successfully. Please verify with OTP sent to your registered mobile number.");
    }


    /**
     * Get payment status (only for payment owner)
     */
    public Optional<Payment> getPaymentStatus(String paymentId, String currentUserId) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();

            // Verify user ownership
            if (!payment.getUserId().equals(currentUserId)) {
                logger.warn("User {} trying to access payment {} owned by {}", currentUserId, paymentId, payment.getUserId());
                return Optional.empty();
            }

            return paymentOpt;
        }

        return Optional.empty();
    }

    public void updatePayment(TransactionCompletedMessage transactionCompletedMessage) {
        logger.info("Updating payment status for paymentId: {}", transactionCompletedMessage.getPaymentId());

        Optional<Payment> paymentOpt = paymentRepository.findById(transactionCompletedMessage.getPaymentId());
        if (paymentOpt.isEmpty()) {
            logger.warn("Payment not found: {}", transactionCompletedMessage.getPaymentId());
            return;
        }
        Payment payment = paymentOpt.get();
        payment.setStatus(PaymentStatus.valueOf(transactionCompletedMessage.getStatus()));
        paymentRepository.save(payment);
        logger.info("Payment status updated successfully for paymentId: {}", transactionCompletedMessage.getPaymentId());
    }

    /**
     * Retry a stuck payment (only for payment owner)
     */
  /*  public boolean retry(String paymentId, String currentUserId) {
        logger.info("Retry requested for payment {} by user {}", paymentId, currentUserId);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            logger.warn("Payment not found: {}", paymentId);
            return false;
        }

        Payment payment = paymentOpt.get();

        // Verify user ownership
        if (!payment.getUserId().equals(currentUserId)) {
            logger.warn("User {} trying to retry payment {} owned by {}", currentUserId, paymentId, payment.getUserId());
            return false;
        }

        // Check if payment can be retried
        if (payment.getStatus() != PaymentStatus.STUCK && payment.getStatus() != PaymentStatus.FAILED) {
            logger.warn("Payment {} is not in retryable status: {}", paymentId, payment.getStatus());
            return false;
        }

        if (payment.getRetryCount() >= 3) {
            logger.warn("Maximum retry attempts exceeded for payment: {}", paymentId);
            return false;
        }

        // Increment retry count and update status
        payment.setRetryCount(payment.getRetryCount() + 1);
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Publish payment retry event
        PaymentRetryEvent retryEvent = new PaymentRetryEvent(paymentId, currentUserId, payment.getRetryCount(), "Manual retry requested by user", Instant.now());
        eventPublisher.publishPaymentRetry(retryEvent);

        // Schedule retry on executor
        retryExecutor.execute(() -> processAsync(paymentId));

        return true;
    }*/


//    @Async("paymentRetryExecutor")
//    public void processAsync(String paymentId) {
//        Optional<Payment> maybe = paymentRepository.findById(paymentId);
//        if (maybe.isEmpty()) {
//            logger.warn("Payment not found for processing: {}", paymentId);
//            return;
//        }
//
//        Payment payment = maybe.get();
//        logger.info("Processing payment asynchronously: {}", paymentId);
//
//        String lockKey = "lock:account:" + payment.getFromAccount();
//        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1");
//
//        if (!Boolean.TRUE.equals(locked)) {
//            logger.warn("Account {} is locked, marking payment as stuck: {}", payment.getFromAccount(), paymentId);
//            payment.setStatus(PaymentStatus.STUCK);
//            paymentRepository.save(payment);
//
//            // Publish payment processed event with STUCK status
//            PaymentProcessedEvent stuckEvent = new PaymentProcessedEvent(paymentId, payment.getUserId(), payment.getFromAccount(), payment.getToAccount(), payment.getAmount(), PaymentStatus.STUCK, payment.getDescription(), null, "Account is locked", Instant.now());
//            eventPublisher.publishPaymentProcessed(stuckEvent);
//            return;
//        }
//
//        try {
//            payment.setStatus(PaymentStatus.PROCESSING);
//            payment.setProcessingStartedAt(Instant.now());
//            paymentRepository.save(payment);
//
//            // Call payment processor adapter
//            var result = adapter.process(payment);
//
//            if (result.success()) {
//                payment.setStatus(PaymentStatus.COMPLETED);
//                payment.setCompletedAt(Instant.now());
//                paymentRepository.save(payment);
//
//                logger.info("Payment completed successfully: {}", paymentId);
//
//                // Publish payment processed event with COMPLETED status
//                PaymentProcessedEvent successEvent = new PaymentProcessedEvent(payment.getPaymentId(), payment.getUserId(), payment.getFromAccount(), payment.getToAccount(), payment.getAmount(), PaymentStatus.COMPLETED, payment.getDescription(), result.providerTxnId(), null, Instant.now());
//                eventPublisher.publishPaymentProcessed(successEvent);
//
//                // Send completion notification
//                notificationPublisher.sendPaymentConfirmationEmail("user@example.com", // Should get from user service
//                        paymentId, payment.getAmount().toString(), "COMPLETED");
//
//            } else {
//                payment.setStatus(PaymentStatus.FAILED);
//                payment.setFailureReason(result.code());
//                payment.setFailedAt(Instant.now());
//                paymentRepository.save(payment);
//
//                logger.error("Payment failed: {} with code: {}", paymentId, result.code());
//
//                // Publish payment processed event with FAILED status
//                PaymentProcessedEvent failedEvent = new PaymentProcessedEvent(paymentId, payment.getUserId(), payment.getFromAccount(), payment.getToAccount(), payment.getAmount(), PaymentStatus.FAILED, payment.getDescription(), result.providerTxnId(), result.code(), Instant.now());
//                eventPublisher.publishPaymentProcessed(failedEvent);
//            }
//
//        } catch (Exception ex) {
//            payment.setStatus(PaymentStatus.STUCK);
//            payment.setRetryCount(payment.getRetryCount() + 1);
//            payment.setFailureReason(ex.getMessage());
//            paymentRepository.save(payment);
//
//            logger.error("Payment processing failed: {} - {}", paymentId, ex.getMessage(), ex);
//
//            // Publish payment processed event with STUCK status
//            PaymentProcessedEvent stuckEvent = new PaymentProcessedEvent(paymentId, payment.getUserId(), payment.getFromAccount(), payment.getToAccount(), payment.getAmount(), PaymentStatus.STUCK, payment.getDescription(), null, ex.getMessage(), Instant.now());
//            eventPublisher.publishPaymentProcessed(stuckEvent);
//
//        } finally {
//            redis.delete(lockKey);
//        }
//    }

    /**
     * Deposit money to account
     */
    @Transactional
    public PaymentInitiatedResponse deposit(String account, BigDecimal amount, String description, String currentUserId) {
        logger.info("Processing deposit for user {} to account {} amount {}", currentUserId, account, amount);

        String paymentId = idGenerator.generateStringId();

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setUserId(currentUserId);
        payment.setFromAccount("SYSTEM_DEPOSIT"); // System account for deposits
        payment.setToAccount(account);
        payment.setAmount(amount);
        payment.setDescription(description != null ? description : "Deposit to account");
        payment.setStatus(PaymentStatus.PENDING_VERIFICATION);
        payment.setCreatedAt(Instant.now());
        payment.setRetryCount(0);

        payment = paymentRepository.save(payment);

        // Generate OTP for verification
        String otp = otpService.generateOtp(paymentId);

        try {
            otpEmailPublisher.publishOtpEmail(new OtpNotificationEvent(currentUserId, amount.toString(), otp));
            logger.info("OTP sent to user {} for deposit paymentId: {}", currentUserId, paymentId);
        } catch (Exception e) {
            logger.error("Failed to send OTP notification for deposit paymentId:{} to userId:{}", paymentId, currentUserId, e);
        }

        logger.info("Deposit initiated successfully with ID: {}", paymentId);

        return new PaymentInitiatedResponse(payment.getPaymentId(),
                payment.getFromAccount(),
                payment.getToAccount(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getCreatedAt(),
                "Deposit initiated successfully. Please verify with OTP sent to your registered mobile number.");
    }

    /**
     * Withdraw money from account
     */
    @Transactional
    public PaymentInitiatedResponse withdraw(String account, BigDecimal amount, String description, String currentUserId) {
        logger.info("Processing withdrawal for user {} from account {} amount {}", currentUserId, account, amount);

        String paymentId = idGenerator.generateStringId();

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setUserId(currentUserId);
        payment.setFromAccount(account);
        payment.setToAccount("SYSTEM_WITHDRAWAL"); // System account for withdrawals
        payment.setAmount(amount);
        payment.setDescription(description != null ? description : "Withdrawal from account");
        payment.setStatus(PaymentStatus.PENDING_VERIFICATION);
        payment.setCreatedAt(Instant.now());
        payment.setRetryCount(0);

        payment = paymentRepository.save(payment);

        // Generate OTP for verification
        String otp = otpService.generateOtp(paymentId);

        try {
            otpEmailPublisher.publishOtpEmail(new OtpNotificationEvent(currentUserId, amount.toString(), otp));
            logger.info("OTP sent to user {} for withdrawal paymentId: {}", currentUserId, paymentId);
        } catch (Exception e) {
            logger.error("Failed to send OTP notification for withdrawal paymentId:{} to userId:{}", paymentId, currentUserId, e);
        }

        logger.info("Withdrawal initiated successfully with ID: {}", paymentId);

        return new PaymentInitiatedResponse(payment.getPaymentId(),
                payment.getFromAccount(),
                payment.getToAccount(),
                payment.getAmount(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getCreatedAt(),
                "Withdrawal initiated successfully. Please verify with OTP sent to your registered mobile number.");
    }

    /**
     * Get payment history for a user
     */
    public org.springframework.data.domain.Page<Payment> getPaymentHistory(String currentUserId, int page, int size) {
        logger.info("Fetching payment history for user {} - page: {}, size: {}", currentUserId, page, size);
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable);
    }

    /**
     * Cancel a pending payment
     */
    @Transactional
    public boolean cancelPayment(String paymentId, String currentUserId) {
        logger.info("Cancel requested for payment {} by user {}", paymentId, currentUserId);

        Optional<Payment> paymentOpt = paymentRepository.findCancellablePayment(paymentId, currentUserId);
        if (paymentOpt.isEmpty()) {
            logger.warn("Payment not found or not cancellable: {} for user: {}", paymentId, currentUserId);
            return false;
        }

        Payment payment = paymentOpt.get();
        
        // Update payment status to failed with cancellation reason
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Payment cancelled by user");
        payment.setFailedAt(Instant.now());
        paymentRepository.save(payment);

        logger.info("Payment {} successfully cancelled by user {}", paymentId, currentUserId);
        return true;
    }

    /**
     * Process bulk transfers
     */
    @Transactional
    public com.fintech.paymentservice.dto.response.BulkTransferResponse processBulkTransfers(
            java.util.List<InitiateRequest> transfers, String currentUserId) {
        
        logger.info("Processing bulk transfer of {} requests for user {}", transfers.size(), currentUserId);
        
        java.util.List<PaymentInitiatedResponse> successful = new java.util.ArrayList<>();
        java.util.List<com.fintech.paymentservice.dto.response.BulkTransferResponse.BulkTransferError> failed = new java.util.ArrayList<>();
        
        for (int i = 0; i < transfers.size(); i++) {
            try {
                InitiateRequest request = transfers.get(i);
                PaymentInitiatedResponse response = initiate(request, currentUserId);
                successful.add(response);
                logger.debug("Bulk transfer {} of {} successful: {}", i + 1, transfers.size(), response.paymentId());
            } catch (Exception e) {
                logger.error("Bulk transfer {} of {} failed: {}", i + 1, transfers.size(), e.getMessage());
                failed.add(new com.fintech.paymentservice.dto.response.BulkTransferResponse.BulkTransferError(
                    i, "Transfer failed", e.getMessage()));
            }
        }
        
        logger.info("Bulk transfer completed for user {}: {} successful, {} failed", 
                   currentUserId, successful.size(), failed.size());
        
        return new com.fintech.paymentservice.dto.response.BulkTransferResponse(
            successful, failed, transfers.size(), successful.size(), failed.size());
    }
}
