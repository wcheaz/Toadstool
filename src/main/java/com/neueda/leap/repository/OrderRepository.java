package com.neueda.leap.repository;

import com.neueda.leap.Order;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Order persistence operations
 * Abstracts the data access layer behind a contract
 */
public interface OrderRepository {
    Order findById(UUID orderId);
    List<Order> findByAccountId(UUID accountId, int limit, int offset);
    Order findByIdempotencyKey(UUID accountId, String idempotencyKey);
    int countByAccountId(UUID accountId);
    void save(UUID accountId, UUID instrumentId, String side, String quantity, String idempotencyKey);
    void updateStatus(UUID orderId, String status);
}
