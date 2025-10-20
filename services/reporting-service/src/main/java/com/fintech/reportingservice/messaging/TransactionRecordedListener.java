package com.fintech.reportingservice.messaging;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.reportingservice.entity.TransactionRecord;
import com.fintech.reportingservice.repository.TransactionRecordRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TransactionRecordedListener {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TransactionRecordRepository repo;

    public TransactionRecordedListener(TransactionRecordRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = "transaction-recorded", groupId = "reporting")
    public void onRecord(Map<String, Object> payload) {
        /*TransactionRecord r = new TransactionRecord();
        r.setPaymentId((String) payload.get("paymentId"));
        r.setStatus((String) payload.get("status"));
        repo.save(r);*/
    }
}
