package com.fintech.ledgerservice.service;

import com.fintech.ledgerservice.dto.message.TransactionCompletedMessage;
import com.fintech.ledgerservice.entity.LedgerEntry;
import com.fintech.ledgerservice.entity.LedgerEntryType;
import com.fintech.ledgerservice.repository.LedgerRepository;
import com.fintech.ledgerservice.util.SnowflakeIdGenerator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class LedgerService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerRepository ledgerRepo;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public LedgerService(LedgerRepository ledgerRepo,
                         SnowflakeIdGenerator snowflakeIdGenerator) {
        this.ledgerRepo = ledgerRepo;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /*   @Transactional
       public String postDoubleEntry(String txnId, String debitAccountId, String creditAccountId, BigDecimal amount) {

           logger.info("Processing double entry for txnId: {}, debit: {}, credit: {}, amount: {}",
               txnId, debitAccountId, creditAccountId, amount);

           // Fetch balances (pessimistic lock can be added)
           AccountBalance debit = accountRepo.findById(debitAccountId)
               .orElseThrow(() -> new IllegalStateException("Payer account not found: " + debitAccountId));

           if (debit.getCurrentBalance().compareTo(amount) < 0) {
               throw new IllegalStateException("Insufficient funds in account: " + debitAccountId);
           }

           AccountBalance credit = accountRepo.findById(creditAccountId).orElseGet(() -> {
               logger.info("Creating new account balance for creditAccountId: {}", creditAccountId);
               AccountBalance a = new AccountBalance();
               a.setBalanceId(snowflakeIdGenerator.nextId());
               a.setAccountId(creditAccountId);
               a.setCurrentBalance(BigDecimal.ZERO);
               a.setAvailableBalance(BigDecimal.ZERO);
               a.setPendingBalance(BigDecimal.ZERO);
               a.setCurrency("USD");
               return a;
           });

           // Create ledger entries with Snowflake IDs
           LedgerEntry debitEntry = new LedgerEntry();
           debitEntry.setEntryId(snowflakeIdGenerator.nextId());
           debitEntry.setTxnId(txnId);
           debitEntry.setAccountId(debitAccountId);
           debitEntry.setEntryType("DEBIT");
           debitEntry.setAmount(amount);
           debitEntry.setCurrency("USD");

           LedgerEntry creditEntry = new LedgerEntry();
           creditEntry.setEntryId(snowflakeIdGenerator.nextId());
           creditEntry.setTxnId(txnId);
           creditEntry.setAccountId(creditAccountId);
           creditEntry.setEntryType("CREDIT");
           creditEntry.setAmount(amount);
           creditEntry.setCurrency("USD");

           // Update balances
           debit.setCurrentBalance(debit.getCurrentBalance().subtract(amount));
           debit.setAvailableBalance(debit.getAvailableBalance().subtract(amount));

           credit.setCurrentBalance(credit.getCurrentBalance().add(amount));
           credit.setAvailableBalance(credit.getAvailableBalance().add(amount));

           // Persist all changes
           ledgerRepo.save(debitEntry);
           ledgerRepo.save(creditEntry);
           accountRepo.save(debit);
           accountRepo.save(credit);

           // Publish event
           LedgerEntryCreatedEvent event = new LedgerEntryCreatedEvent(
               snowflakeIdGenerator.nextId(),
               txnId,
               debitAccountId,
               creditAccountId,
               amount,
               "USD"
           );
           eventPublisher.publishLedgerEntryCreated(event);

           logger.info("Successfully processed double entry for txnId: {}", txnId);

           return txnId;
       }

       public AccountBalanceResponse getAccountBalance(String accountId) {
           logger.info("Fetching balance for accountId: {}", accountId);

           AccountBalance balance = accountRepo.findById(accountId)
               .orElseThrow(() -> new IllegalStateException("Account not found: " + accountId));

           return new AccountBalanceResponse(
               balance.getBalanceId(),
               balance.getAccountId(),
               balance.getCurrentBalance(),
               balance.getAvailableBalance(),
               balance.getPendingBalance(),
               balance.getCurrency()
           );
       }
   */
    @Transactional
    public void createLedgerEntry(TransactionCompletedMessage transactionCompletedMessage) {

        logger.info("Processing double entry for txnId: {}, debit: {}, credit: {}, amount: {}",
                transactionCompletedMessage.getTxnId(),
                transactionCompletedMessage.getFromAccount(),
                transactionCompletedMessage.getToAccount(),
                transactionCompletedMessage.getAmount());

        // Create ledger entries with Snowflake IDs
        LedgerEntry debitEntry = new LedgerEntry(snowflakeIdGenerator.nextId(),
                transactionCompletedMessage.getTxnId(),
                transactionCompletedMessage.getPaymentId(),
                transactionCompletedMessage.getFromAccount(),
                LedgerEntryType.DEBIT,
                transactionCompletedMessage.getAmount(),
                transactionCompletedMessage.getDescription());

        LedgerEntry creditEntry = new LedgerEntry(
                snowflakeIdGenerator.nextId(),
                transactionCompletedMessage.getTxnId(),
                transactionCompletedMessage.getPaymentId(),
                transactionCompletedMessage.getToAccount(),
                LedgerEntryType.CREDIT,
                transactionCompletedMessage.getAmount(),
                transactionCompletedMessage.getDescription()
        );

        // Persist all changes
        ledgerRepo.save(debitEntry);
        ledgerRepo.save(creditEntry);

        logger.info("Successfully processed double entry for txnId: {}", transactionCompletedMessage.getTxnId());

    }
}
