package com.neueda.leap.service;

import com.neueda.leap.mapper.AccountMapper;
import com.neueda.leap.mapper.CashEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CashEventService {
    private final AccountMapper accountMapper;
    private final CashEventMapper cashEventMapper;

    public CashEventService(AccountMapper accountMapper, CashEventMapper cashEventMapper) {
        this.accountMapper = accountMapper;
        this.cashEventMapper = cashEventMapper;
    }

    @Transactional
    public void post(UUID accountId, UUID orderId, String eventType, BigDecimal amount, String details) {
        if (amount == null || amount.signum() == 0) {
            throw new IllegalArgumentException("Cash event amount must not be zero");
        }
        if (accountMapper.applyBalanceDelta(accountId, amount) != 1) {
            throw new IllegalArgumentException("Insufficient balance or account not found");
        }
        cashEventMapper.insert(accountId, orderId, eventType, amount, details == null ? "{}" : details);
    }
}