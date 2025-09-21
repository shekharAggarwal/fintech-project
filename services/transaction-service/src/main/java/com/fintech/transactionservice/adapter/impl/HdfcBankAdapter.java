package com.fintech.transactionservice.adapter.impl;

import com.fintech.transactionservice.adapter.BankAdapter;
import com.fintech.transactionservice.annotation.BankCode;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.model.TransactionResult;
import org.springframework.stereotype.Service;

@Service
@BankCode("Self")
public class HdfcBankAdapter implements BankAdapter {

    @Override
    public TransactionResult process(Transaction payment) {
        double r = Math.random();
        if (r < 0.7) {
            return new TransactionResult(true, "OK", "TXN-" + payment.getTxnId());
        } else if (r < 0.95) {
            return new TransactionResult(false, "INSUFFICIENT_FUNDS", null);
        } else {
            // simulate timeout / stuck
            throw new RuntimeException("provider-timeout");
        }
    }
}