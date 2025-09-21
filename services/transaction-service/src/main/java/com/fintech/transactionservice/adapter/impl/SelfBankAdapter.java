package com.fintech.transactionservice.adapter.impl;


import com.fintech.transactionservice.adapter.BankAdapter;
import com.fintech.transactionservice.annotation.BankCode;
import com.fintech.transactionservice.entity.Account;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.model.TransactionResult;
import com.fintech.transactionservice.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@BankCode("Self")
public class SelfBankAdapter implements BankAdapter {

    final AccountRepository accountRepository;

    public SelfBankAdapter(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }


    @Override
    public TransactionResult process(Transaction transaction) {

        // Lock accounts in consistent order to prevent deadlocks
        String first, second;
        if (transaction.getFromAccount().compareTo(transaction.getToAccount()) < 0) {
            first = transaction.getFromAccount();
            second = transaction.getToAccount();
        } else {
            first = transaction.getToAccount();
            second = transaction.getFromAccount();
        }

        Account acc1 = accountRepository.lockAccount(first).orElseThrow();
        Account acc2 = accountRepository.lockAccount(second).orElseThrow();

        Account sender = (acc1.getAccountNumber().equals(transaction.getFromAccount())) ? acc1 : acc2;
        Account receiver = (sender == acc1) ? acc2 : acc1;
        if (sender.getBalance().compareTo(transaction.getAmount()) < 0) {
            return new TransactionResult(false, "INSUFFICIENT_FUNDS", transaction.getTxnId());
        }

        sender.setBalance(sender.getBalance().subtract(transaction.getAmount()));
        receiver.setBalance(receiver.getBalance().add(transaction.getAmount()));

        // Save both accounts in a single database call
        accountRepository.saveAll(List.of(sender, receiver));

        return new TransactionResult(true, "SUCCESS", transaction.getTxnId());
    }
}