package com.fintech.paymentservice.service;

import com.fintech.paymentservice.dto.message.PaymentInitiatedEvent;
import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.messaging.TransactionPublisher;
import com.fintech.paymentservice.model.PaymentStatus;
import com.fintech.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final PaymentRepository paymentRepository;

    private final TransactionPublisher transactionPublisher;

    private static final String OTP_KEY_PREFIX = "otp:payment:";
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_LENGTH = 6;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;

    public OtpService(PaymentRepository paymentRepository, TransactionPublisher transactionPublisher, StringRedisTemplate redisTemplate) {
        this.paymentRepository = paymentRepository;
        this.transactionPublisher = transactionPublisher;
        this.redisTemplate = redisTemplate;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate and store OTP for payment verification
     */
    public String generateOtp(String paymentId) {
        String otp = generateRandomOtp();
        String key = OTP_KEY_PREFIX + paymentId;

        // Store OTP in Redis with expiry
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(OTP_EXPIRY_MINUTES));

        logger.info("OTP generated for payment: {} (expires in {} minutes)", paymentId, OTP_EXPIRY_MINUTES);
        return otp;
    }

    /**
     * Validate OTP for payment
     */
    private boolean validateOtp(String paymentId, String providedOtp) {
        String key = OTP_KEY_PREFIX + paymentId;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            logger.warn("OTP not found or expired for payment: {}", paymentId);
            return false;
        }

        boolean isValid = storedOtp.equals(providedOtp);

        if (isValid) {
            // Delete OTP after successful verification
            redisTemplate.delete(key);
            logger.info("OTP verified successfully for payment: {}", paymentId);
        } else {
            logger.warn("Invalid OTP provided for payment: {}", paymentId);
        }

        return isValid;
    }

    /**
     * Verify OTP and authorize payment
     */
    @Transactional
    public boolean verifyOtp(String paymentId, String providedOtp, String currentUserId) {
        logger.info("Verifying OTP for payment {} by user {}", paymentId, currentUserId);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            logger.warn("Payment not found: {}", paymentId);
            return false;
        }

        Payment payment = paymentOpt.get();

        // Verify user ownership
        if (!payment.getUserId().equals(currentUserId)) {
            logger.warn("User {} trying to verify OTP for payment {} owned by {}", currentUserId, paymentId, payment.getUserId());
            return false;
        }

        // Check payment status
        if (payment.getStatus() != PaymentStatus.PENDING_VERIFICATION) {
            logger.warn("Payment {} is not in pending verification status: {}", paymentId, payment.getStatus());
            return false;
        }

        // Verify OTP
        boolean otpValid = validateOtp(paymentId, providedOtp);
        if (!otpValid) {
            logger.warn("Invalid OTP provided for payment: {}", paymentId);

            //fail the payment
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid OTP");
            payment.setFailedAt(Instant.now());
            paymentRepository.save(payment);

            return false;
        }

        // Update payment status to authorized
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setAuthorizedAt(Instant.now());
        paymentRepository.save(payment);
        logger.info("OTP verified successfully for payment: {}", paymentId);

        try { // Publish OTP verified event
            PaymentInitiatedEvent paymentInitiatedEvent = new PaymentInitiatedEvent(payment.getPaymentId(),
                    payment.getUserId(), payment.getFromAccount(),
                    payment.getToAccount(), payment.getAmount(),
                    payment.getDescription());
            transactionPublisher.publishTransactionInitiate(paymentInitiatedEvent);
            return true;
        } catch (Exception e) {
            logger.error("Error while sending event to transaction service with paymentId: {}", paymentId, e);
            return false;
        }
    }

    /**
     * Check if OTP exists for payment
     */
    public boolean otpExists(String paymentId) {
        String key = OTP_KEY_PREFIX + paymentId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Get remaining OTP expiry time in seconds
     */
    public long getOtpExpiryTime(String paymentId) {
        String key = OTP_KEY_PREFIX + paymentId;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    private String generateRandomOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}