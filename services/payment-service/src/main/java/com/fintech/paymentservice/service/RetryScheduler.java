package com.fintech.paymentservice.service;


import org.springframework.stereotype.Component;

@Component
public class RetryScheduler {

  /*  private final PaymentRepository repo;
    private final PaymentService paymentService;
    @Value("${payment.stuck.processingTimeoutSeconds:120}")
    private long processingTimeout;

    public RetryScheduler(PaymentRepository repo, PaymentService paymentService) {
        this.repo = repo;
        this.paymentService = paymentService;
    }

    *//**
     * Runs every 1 minute to pick stuck or processing payments older than threshold
     *//*
    @Scheduled(fixedDelayString = "${payment.scheduler.checkIntervalMs:60000}")
    public void scanAndRetry() {
        Instant cutoff = Instant.now().minusSeconds(processingTimeout);
        // Find PROCESSING older than cutoff (likely stuck)
        List<Payment> stuck = repo.findByStatusAndProcessingStartedAtBefore(PaymentStatus.PROCESSING, cutoff);
        for (Payment p : stuck) {
            // mark as STUCK and hand over to retry service by calling retryPayment
            p.setStatus(PaymentStatus.STUCK);
            p.setUpdatedAt(Instant.now());
            repo.save(p);
            paymentService.retry(p.getPaymentId());
        }

        // Optionally check PENDING older than some threshold
        List<Payment> pending = repo.findByStatus(PaymentStatus.PENDING);
        for (Payment p : pending) {
            if (p.getCreatedAt().isBefore(Instant.now().minusSeconds(processingTimeout * 2))) {
                p.setStatus(PaymentStatus.STUCK);
                p.setUpdatedAt(Instant.now());
                repo.save(p);
                paymentService.retry(p.getPaymentId());
            }
        }
    }*/
}
