package com.neueda.leap.repository;

import com.neueda.leap.Fill;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Fill persistence operations
 */
public interface FillRepository {
    Fill findById(UUID fillId);
    List<Fill> findByOrderId(UUID orderId);
    List<Fill> findByOrderIdPaginated(UUID orderId, int limit, int offset);
    int countByOrderId(UUID orderId);
    List<Fill> findByAccountId(UUID accountId, int limit, int offset);
    int countByAccountId(UUID accountId);
    void save(UUID orderId, String price, String quantity, String status);
}
