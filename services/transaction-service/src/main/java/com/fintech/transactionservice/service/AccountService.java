package com.fintech.transactionservice.service;

import com.fintech.transactionservice.entity.Account;
import com.fintech.transactionservice.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountService {

    final private AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void createAccount(String userId, String accountNumber, BigDecimal balance) {
        Account account = new Account(userId, accountNumber, balance);
        accountRepository.save(account);
    }
}
