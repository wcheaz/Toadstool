package com.neueda.leap.service;

import com.neueda.leap.Fill;
import com.neueda.leap.mapper.FillMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Fill operations
 */
@Service
public class FillService {

    private final FillMapper fillMapper;

    public FillService(FillMapper fillMapper) {
        this.fillMapper = fillMapper;
    }

    /**
     * Get fill by ID
     */
    public Fill getFillById(UUID fillId) {
        return fillMapper.selectFillById(fillId);
    }

    /**
     * List fills for an order
     */
    public List<Fill> listFillsByOrder(UUID orderId) {
        return fillMapper.selectFillsByOrderId(orderId);
    }

    /**
     * List fills for an order with pagination
     */
    public List<Fill> listFillsByOrderPaginated(UUID orderId, int limit, int offset) {
        return fillMapper.selectFillsByOrderIdPaginated(orderId, limit, offset);
    }

    /**
     * Count fills for an order
     */
    public int countFillsByOrder(UUID orderId) {
        return fillMapper.countFillsByOrderId(orderId);
    }

    /**
     * List fills for an account with pagination
     */
    public List<Fill> listFillsByAccount(UUID accountId, int limit, int offset) {
        return fillMapper.selectFillsByAccountId(accountId, limit, offset);
    }

    /**
     * Count fills for an account
     */
    public int countFillsByAccount(UUID accountId) {
        return fillMapper.countFillsByAccountId(accountId);
    }

    /**
     * Create a new fill (used by order execution engine)
     */
    public void createFill(UUID orderId, String price, String quantity, String status) {
        validateFillInput(price, quantity, status);
        fillMapper.insertFill(orderId, price, quantity, status);
    }

    /**
     * Validate fill input
     */
    private void validateFillInput(String price, String quantity, String status) {
        if (price == null || status == null || quantity == null) {
            throw new IllegalArgumentException("Price, quantity, and status are required");
        }

        try {
            java.math.BigDecimal p = new java.math.BigDecimal(price);
            if (p.compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price must be >= 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Price must be a valid decimal number");
        }

        if (!status.equals("Filled") && !status.equals("Failed") && !status.equals("Pending")) {
            throw new IllegalArgumentException("Invalid fill status");
        }
    }
}
