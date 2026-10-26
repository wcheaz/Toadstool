package com.neueda.leap.repository.impl;

import com.neueda.leap.Order;
import com.neueda.leap.repository.OrderRepository;
import com.neueda.leap.mapper.OrderMapper;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis-based implementation of OrderRepository
 * Wraps OrderMapper to comply with repository interface
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;

    public OrderRepositoryImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Order findById(UUID orderId) {
        return orderMapper.selectOrderById(orderId);
    }

    @Override
    public List<Order> findByAccountId(UUID accountId, int limit, int offset) {
        return orderMapper.selectOrdersByAccountId(accountId, limit, offset);
    }

    @Override
    public List<Order> findAll(OffsetDateTime fromSubmittedAt, OffsetDateTime toSubmittedAt, int limit, int offset) {
        return orderMapper.selectAllOrders(fromSubmittedAt, toSubmittedAt, limit, offset);
    }

    @Override
    public Order findByIdempotencyKey(UUID accountId, String idempotencyKey) {
        return orderMapper.selectOrderByIdempotencyKey(accountId, idempotencyKey);
    }

    @Override
    public int countByAccountId(UUID accountId) {
        return orderMapper.countOrdersByAccountId(accountId);
    }

    @Override
    public int countAll(OffsetDateTime fromSubmittedAt, OffsetDateTime toSubmittedAt) {
        return orderMapper.countAllOrders(fromSubmittedAt, toSubmittedAt);
    }

    @Override
    public void save(UUID accountId, UUID instrumentId, String side, String quantity, String idempotencyKey) {
        save(accountId, instrumentId, side, quantity, idempotencyKey, "MARKET", null);
    }

    @Override
    public void save(UUID accountId, UUID instrumentId, String side, String quantity, String idempotencyKey,
                     String orderType, BigDecimal limitPrice) {
        orderMapper.insertOrder(accountId, instrumentId, side, quantity, idempotencyKey, orderType, limitPrice);
    }

    @Override
    public List<Order> findUnexecuted(OffsetDateTime submittedBefore, int limit) {
        return orderMapper.selectUnexecutedOrders(submittedBefore, limit);
    }

    @Override
    public BigDecimal pendingSellQuantity(UUID accountId, UUID instrumentId) {
        return orderMapper.sumPendingSellQuantity(accountId, instrumentId);
    }
}
