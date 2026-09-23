package com.neueda.leap.service;

import com.neueda.leap.Order;
import com.neueda.leap.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Order operations
 * Handles business logic, validation, and database access
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * Retrieve a single order by ID
     */
    public Order getOrderById(UUID orderId) {
        return orderMapper.selectOrderById(orderId);
    }

    /**
     * List orders for an account with pagination
     */
    public List<Order> listOrdersByAccount(UUID accountId, int limit, int offset) {
        return orderMapper.selectOrdersByAccountId(accountId, limit, offset);
    }

    /**
     * Get total count of orders for an account
     */
    public int countOrdersByAccount(UUID accountId) {
        return orderMapper.countOrdersByAccountId(accountId);
    }

    /**
     * Check if an order with the given idempotency key already exists for the account
     */
    public Order getOrderByIdempotencyKey(UUID accountId, String idempotencyKey) {
        return orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey);
    }

    /**
     * Create a new order
     * Validates input and persists to database
     */
    public Order createOrder(UUID accountId, UUID instrumentId, String side, String quantity, String idempotencyKey) {
        // Validate input
        validateOrderInput(side, quantity);

        // Insert order
        orderMapper.insertOrder(accountId, instrumentId, side, quantity, idempotencyKey);

        // Retrieve and return the created order
        return getOrderByIdempotencyKey(accountId, idempotencyKey);
    }

    /**
     * Validate order input
     */
    private void validateOrderInput(String side, String quantity) {
        if (side == null || (!side.equals("BUY") && !side.equals("SELL"))) {
            throw new IllegalArgumentException("Invalid order side. Must be BUY or SELL.");
        }

        try {
            java.math.BigDecimal qty = new java.math.BigDecimal(quantity);
            if (qty.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a valid decimal number");
        }
    }

    /**
     * Update order status (used internally for state transitions)
     */
    public void updateOrderStatus(UUID orderId, String status) {
        orderMapper.updateOrderStatus(orderId, status);
    }
}
