package com.core.AxiomBank.Services;

import com.core.AxiomBank.Dtos.SetAmountReq;
import com.core.AxiomBank.Dtos.TransactionResp;
import com.core.AxiomBank.Entities.*;
import com.core.AxiomBank.Exceptions.BadRequestException;
import com.core.AxiomBank.Exceptions.ForbiddenActionException;
import com.core.AxiomBank.Exceptions.NotFoundException;
import com.core.AxiomBank.Redis.RateLimitingService;
import com.core.AxiomBank.Repositories.AccountRepository;
import com.core.AxiomBank.Repositories.LedgerEntryRepository;
import com.core.AxiomBank.Repositories.Mappers;
import com.core.AxiomBank.Repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final RateLimitingService rateLimitingService;
    private final Mappers mappers;

    @Autowired
    @Lazy
    private TransactionService self; // Used to invoke methods with different Transactional scopes

    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("3.0");
    private static final Map<String, BigDecimal> CURRENCY_LIMITS = Map.of(
            "USD", new BigDecimal("10000"),
            "EUR", new BigDecimal("9200"),
            "GBP", new BigDecimal("7800"),
            "CHF", new BigDecimal("8600"),
            "PLN", new BigDecimal("40000")
    );


    public TransactionResp depositTransaction(Long accountId, SetAmountReq req, Long clientId) {
        log.info("Request: DEPOSIT | Account: {} | Amount: {}", accountId, req.getAmount());

        // 1. Pre-Database Checks (Performance & Security)
        validateAmount(req.getAmount());
        rateLimitingService.checkTransactionFrequency(clientId);


        Transaction transaction = self.initTransaction(TransactionType.DEPOSIT, req.getAmount(), accountId, null);

        try {
            // 3. Execute Business Logic (Locked & Rolled back on failure)
            self.processDeposit(transaction.getId(), accountId, req.getAmount());

            // 4. Update Audit Log to Completed
            return self.finalizeTransaction(transaction.getId(), TransactionStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Deposit Failed", e);
            self.markTransactionFailed(transaction.getId(), e.getMessage());
            throw e;
        }
    }

    public TransactionResp withdrawTransaction(Long clientId, Long accountId, SetAmountReq req) {
        log.info("Request: WITHDRAW | Client: {} | Account: {}", clientId, accountId);

        validateAmount(req.getAmount());
        rateLimitingService.checkTransactionFrequency(clientId);

        Transaction transaction = self.initTransaction(TransactionType.WITHDRAWAL, req.getAmount(), null, accountId);

        try {
            self.processWithdrawal(transaction.getId(), clientId, accountId, req.getAmount());
            return self.finalizeTransaction(transaction.getId(), TransactionStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Withdrawal Failed", e);
            self.markTransactionFailed(transaction.getId(), e.getMessage());
            throw e;
        }
    }

    public TransactionResp transferTransaction(Long fromId, Long ownerId, Long toId, SetAmountReq req) {
        log.info("Request: TRANSFER | From: {} | To: {}", fromId, toId);

        validateAmount(req.getAmount());
        rateLimitingService.checkTransactionFrequency(ownerId);

        if (fromId.equals(toId)) throw new BadRequestException("Cannot transfer to same account.");

        // Create transaction linked to both accounts
        Transaction transaction = self.initTransaction(TransactionType.TRANSFER, req.getAmount(), toId, fromId);

        try {
            self.processTransfer(transaction.getId(), ownerId, fromId, toId, req.getAmount());
            return self.finalizeTransaction(transaction.getId(), TransactionStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Transfer Failed", e);
            self.markTransactionFailed(transaction.getId(), e.getMessage());
            throw e;
        }
    }

    // ==================================================================================
    // TRANSACTIONAL SEGMENTS
    // ==================================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction initTransaction(TransactionType type, BigDecimal amount, Long toId, Long fromId) {
        Account to = (toId != null) ? accountRepository.findById(toId).orElse(null) : null;
        Account from = (fromId != null) ? accountRepository.findById(fromId).orElse(null) : null;

        Transaction tx = new Transaction();
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.PENDING);
        tx.setReferenceNumber(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setToAccount(to);
        tx.setFromAccount(from);
        return transactionRepository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDeposit(Long txId, Long accountId, BigDecimal amount) {
        // PESSIMISTIC LOCKING: Waits here if another transaction is writing
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        validateAccountState(account);
        validateLimits(account, amount);

        // Logic: Calculate Fee -> Deduct Fee -> Add Remainder
        BigDecimal fee = calculateFee(amount);
        BigDecimal realDeposit = amount.subtract(fee); // Payer pays fee from the deposit amount


        account.setBalance(account.getBalance().add(realDeposit));

        accountRepository.save(account);

        // Record Fee & Ledger
        recordFee(account, fee);

        Transaction tx = transactionRepository.getReferenceById(txId);
        createLedgerEntry(tx, realDeposit, account, EntryType.CREDIT);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processWithdrawal(Long txId, Long clientId, Long accountId, BigDecimal amount) {
        // PESSIMISTIC LOCKING
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (!account.getOwner().getId().equals(clientId)) {
            throw new ForbiddenActionException("Access denied.");
        }

        validateAccountState(account);
        validateLimits(account, amount);

        BigDecimal fee = calculateFee(amount);
        BigDecimal totalDeduction = amount.add(fee);

        checkBalance(account, totalDeduction);

        // Update Balance
        account.setBalance(account.getBalance().subtract(totalDeduction));
        accountRepository.save(account);

        recordFee(account, fee);

        // Ledger: Debit the account
        Transaction tx = transactionRepository.getReferenceById(txId);
        createLedgerEntry(tx, amount.negate(), account, EntryType.DEBIT);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTransfer(Long txId, Long ownerId, Long fromId, Long toId, BigDecimal amount) {
        // DEADLOCK PREVENTION
        Account firstLock;
        Account secondLock;

        if (fromId < toId) {
            firstLock = accountRepository.findByIdWithLock(fromId).orElseThrow();
            secondLock = accountRepository.findByIdWithLock(toId).orElseThrow();
        } else {
            firstLock = accountRepository.findByIdWithLock(toId).orElseThrow();
            secondLock = accountRepository.findByIdWithLock(fromId).orElseThrow();
        }


        Account fromAccount = (fromId.equals(firstLock.getId())) ? firstLock : secondLock;
        Account toAccount = (toId.equals(firstLock.getId())) ? firstLock : secondLock;


        if (!fromAccount.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenActionException("Access denied.");
        }
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new BadRequestException("Currency mismatch.");
        }

        validateAccountState(fromAccount);
        validateAccountState(toAccount);

        BigDecimal fee = calculateFee(amount);
        BigDecimal totalDeduction = amount.add(fee);

        checkBalance(fromAccount, totalDeduction);

        // Execute Transfer
        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDeduction));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        recordFee(fromAccount, fee);

        // Ledger
        Transaction tx = transactionRepository.getReferenceById(txId);
        createLedgerEntry(tx, amount.negate(), fromAccount, EntryType.DEBIT);
        createLedgerEntry(tx, amount, toAccount, EntryType.CREDIT);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionResp finalizeTransaction(Long txId, TransactionStatus status) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new NotFoundException("Transaction log lost."));
        tx.setStatus(status);
        return mappers.toTransactionDto(transactionRepository.save(tx));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markTransactionFailed(Long txId, String reason) {
        transactionRepository.findById(txId).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.FAILED);

            transactionRepository.save(tx);
        });
    }

    // ==================================================================================
    // HELPERS
    // ==================================================================================

    private void recordFee(Account payer, BigDecimal feeAmount) {
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTx = new Transaction();
            feeTx.setTransactionType(TransactionType.FEE);
            feeTx.setAmount(feeAmount);
            feeTx.setFromAccount(payer);
            feeTx.setStatus(TransactionStatus.COMPLETED);
            feeTx.setReferenceNumber("FEE-" + UUID.randomUUID().toString().substring(0, 8));
            transactionRepository.save(feeTx);

            createLedgerEntry(feeTx, feeAmount.negate(), payer, EntryType.DEBIT);
        }
    }

    private void createLedgerEntry(Transaction tx, BigDecimal amount, Account account, EntryType type) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAmount(amount);
        entry.setAccount(account);
        entry.setTransaction(tx);
        entry.setEntryType(type);
        ledgerEntryRepository.save(entry);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be positive.");
        }
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(FEE_PERCENTAGE).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private void validateAccountState(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenActionException("Account is not active.");
        }
    }

    private void checkBalance(Account account, BigDecimal requiredAmount) {
        if (account.getBalance().compareTo(requiredAmount) < 0) {
            throw new BadRequestException("Insufficient funds.");
        }
    }

    private void validateLimits(Account account, BigDecimal amount) {
        BigDecimal limit = CURRENCY_LIMITS.getOrDefault(account.getCurrency().name(), new BigDecimal("1000"));
        if (amount.compareTo(limit) > 0) {
            throw new BadRequestException("Amount exceeds limit for currency " + account.getCurrency());
        }
    }
}