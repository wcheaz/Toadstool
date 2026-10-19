package com.neueda.leap.repository;

import com.neueda.leap.Order;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Order persistence operations
 * Abstracts the data access layer behind a contract
 */
public interface OrderRepository {
    Order findById(UUID orderId);
    List<Order> findByAccountId(UUID accountId, int limit, int offset);
    List<Order> findAll(OffsetDateTime fromSubmittedAt, OffsetDateTime toSubmittedAt, int limit, int offset);
    Order findByIdempotencyKey(UUID accountId, String idempotencyKey);
    int countByAccountId(UUID accountId);
    int countAll(OffsetDateTime fromSubmittedAt, OffsetDateTime toSubmittedAt);
    void save(UUID accountId, UUID instrumentId, String side, String quantity, String idempotencyKey);
}
