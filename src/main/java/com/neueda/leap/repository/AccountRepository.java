package com.neueda.leap.repository;

import com.neueda.leap.Account;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Account persistence operations
 */
public interface AccountRepository {
    Account findById(UUID accountId);
    List<Account> findByClientId(UUID clientId, int limit, int offset);
    int countByClientId(UUID clientId);
    void save(UUID clientId, String status);
}
