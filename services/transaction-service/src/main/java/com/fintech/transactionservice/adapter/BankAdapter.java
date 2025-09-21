package com.fintech.transactionservice.adapter;

import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.model.TransactionResult;

public interface BankAdapter {
    TransactionResult process(Transaction payment);
}