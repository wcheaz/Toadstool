package com.neueda.leap.service;

import com.neueda.leap.Account;
import com.neueda.leap.repository.AccountRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Account operations
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Get account by ID
     */
    public Account getAccountById(UUID accountId) {
        return accountRepository.findById(accountId);
    }

    /**
     * List accounts for a client with pagination
     */
    public List<Account> listAccountsByClient(UUID clientId, int limit, int offset) {
        return accountRepository.findByClientId(clientId, limit, offset);
    }

    /**
     * Count accounts for a client
     */
    public int countAccountsByClient(UUID clientId) {
        return accountRepository.countByClientId(clientId);
    }

    /**
     * Create a new account for a client
     */
    public Account createAccount(UUID clientId) {
        return createAccount(clientId, "Main Account");
    }

    public Account createAccount(UUID clientId, String accountName) {
        String normalizedName = accountName == null || accountName.isBlank() ? "Main Account" : accountName.trim();
        accountRepository.save(clientId, normalizedName, "ACTIVE");
        int accountCount = accountRepository.countByClientId(clientId);
        List<Account> accounts = accountRepository.findByClientId(clientId, 1, accountCount - 1);
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    /**
     * Verify that the account belongs to the given client
     * Used for authorization checks
     */
    public boolean verifyAccountOwnership(UUID accountId, UUID clientId) {
        Account account = getAccountById(accountId);
        return account != null && account.getClientId().equals(clientId);
    }

    /**
     * Update account balance (used when orders are filled)
     */
    public void updateAccountBalance(UUID accountId, BigDecimal newBalance) {
        accountRepository.updateBalance(accountId, newBalance);
    }
}
