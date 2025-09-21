package com.fintech.retryservice;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RetryScheduler {
    private final RestTemplate rest;

    public RetryScheduler(RestTemplate rest) {
        this.rest = rest;
    }

    // run every 1 minute
    @Scheduled(fixedDelayString = "${retry.scan.ms:60000}")
    public void scan() {
        // Call a Payment Service endpoint that returns stuck payments
        try {
            var stuck = rest.getForObject("http://payment-service:8082/payments/stuck", java.util.List.class);
            if (stuck != null) {
                for (Object item : stuck) {
                    String paymentId = (String) ((java.util.Map) item).get("paymentId");
                    rest.postForEntity("http://payment-service:8082/payments/" + paymentId + "/retry", null, Void.class);
                }
            }
        } catch (Exception ex) {
            // log and continue
        }
    }
}