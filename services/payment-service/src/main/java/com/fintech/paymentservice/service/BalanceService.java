package com.fintech.paymentservice.service;

import com.fintech.paymentservice.dto.response.BalanceResponse;
import com.fintech.paymentservice.entity.Account;
import com.fintech.paymentservice.exception.AccountNotFoundException;
import com.fintech.paymentservice.exception.InsufficientFundsException;
import com.fintech.paymentservice.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class BalanceService {

    private static final Logger logger = LoggerFactory.getLogger(BalanceService.class);

    private final AccountRepository accountRepository;

    public BalanceService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Get balance for an account
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountNumber) {
        Account account = findAccountOrThrow(accountNumber);
        return toBalanceResponse(account);
    }

    /**
     * Credit (add funds) to an account
     */
    @Transactional
    public BalanceResponse credit(String accountNumber, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        logger.info("Crediting {} to account {}", amount, accountNumber);

        Account account = findAccountOrThrow(accountNumber);

        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setLastBalanceUpdate(Instant.now());

        account = accountRepository.save(account);
        logger.info("Credit successful for account {}. New balance: {}", accountNumber, account.getCurrentBalance());

        return toBalanceResponse(account);
    }

    /**
     * Debit (remove funds) from an account
     */
    @Transactional
    public BalanceResponse debit(String accountNumber, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        logger.info("Debiting {} from account {}", amount, accountNumber);

        Account account = findAccountOrThrow(accountNumber);

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, account.getCurrency(),
                "Insufficient available balance. Available: " + account.getAvailableBalance() + ", Requested: " + amount);
        }

        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setLastBalanceUpdate(Instant.now());

        account = accountRepository.save(account);
        logger.info("Debit successful for account {}. New balance: {}", accountNumber, account.getCurrentBalance());

        return toBalanceResponse(account);
    }

    /**
     * Place a hold on funds (reduces available balance but not current balance)
     */
    @Transactional
    public BalanceResponse placeHold(String accountNumber, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        logger.info("Placing hold of {} on account {}", amount, accountNumber);

        Account account = findAccountOrThrow(accountNumber);

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, account.getCurrency(),
                "Insufficient available balance for hold. Available: " + account.getAvailableBalance() + ", Requested: " + amount);
        }

        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setHoldAmount(account.getHoldAmount().add(amount));
        account.setLastBalanceUpdate(Instant.now());

        account = accountRepository.save(account);
        logger.info("Hold placed successfully on account {}. Hold amount: {}", accountNumber, account.getHoldAmount());

        return toBalanceResponse(account);
    }

    /**
     * Release a hold on funds (increases available balance)
     */
    @Transactional
    public BalanceResponse releaseHold(String accountNumber, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        logger.info("Releasing hold of {} on account {}", amount, accountNumber);

        Account account = findAccountOrThrow(accountNumber);

        BigDecimal actualRelease = amount.min(account.getHoldAmount());

        account.setAvailableBalance(account.getAvailableBalance().add(actualRelease));
        account.setHoldAmount(account.getHoldAmount().subtract(actualRelease));
        account.setLastBalanceUpdate(Instant.now());

        account = accountRepository.save(account);
        logger.info("Hold released successfully on account {}. Remaining hold: {}", accountNumber, account.getHoldAmount());

        return toBalanceResponse(account);
    }

    /**
     * Check if account has sufficient funds for a given amount
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientFunds(String accountNumber, BigDecimal amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElse(null);

        if (account == null) {
            return false;
        }

        return account.getAvailableBalance().compareTo(amount) >= 0;
    }

    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private BalanceResponse toBalanceResponse(Account account) {
        return new BalanceResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getCurrentBalance(),
            account.getAvailableBalance(),
            account.getHoldAmount(),
            account.getCurrency(),
            account.getLastBalanceUpdate()
        );
    }
}
