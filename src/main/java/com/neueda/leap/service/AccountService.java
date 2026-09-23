package com.neueda.leap.service;

import com.neueda.leap.Account;
import com.neueda.leap.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Account operations
 */
@Service
public class AccountService {

    private final AccountMapper accountMapper;

    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    /**
     * Get account by ID
     */
    public Account getAccountById(UUID accountId) {
        return accountMapper.selectAccountById(accountId);
    }

    /**
     * List accounts for a client with pagination
     */
    public List<Account> listAccountsByClient(UUID clientId, int limit, int offset) {
        return accountMapper.selectAccountsByClientId(clientId, limit, offset);
    }

    /**
     * Count accounts for a client
     */
    public int countAccountsByClient(UUID clientId) {
        return accountMapper.countAccountsByClientId(clientId);
    }

    /**
     * Create a new account for a client
     */
    public Account createAccount(UUID clientId) {
        accountMapper.insertAccount(clientId, "ACTIVE");
        // Retrieve the most recent account for this client
        List<Account> accounts = accountMapper.selectAccountsByClientId(clientId, 1, 0);
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
}
