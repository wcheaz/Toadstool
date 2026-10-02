package com.neueda.leap.repository.impl;

import com.neueda.leap.Account;
import com.neueda.leap.repository.AccountRepository;
import com.neueda.leap.mapper.AccountMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis-based implementation of AccountRepository
 */
@Repository
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountMapper accountMapper;

    public AccountRepositoryImpl(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    public Account findById(UUID accountId) {
        return accountMapper.selectAccountById(accountId);
    }

    @Override
    public List<Account> findByClientId(UUID clientId, int limit, int offset) {
        return accountMapper.selectAccountsByClientId(clientId, limit, offset);
    }

    @Override
    public int countByClientId(UUID clientId) {
        return accountMapper.countAccountsByClientId(clientId);
    }

    @Override
    public void save(UUID clientId, String status) {
        accountMapper.insertAccount(clientId, status);
    }
}
