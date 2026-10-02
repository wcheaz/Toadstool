package com.neueda.leap.repository.impl;

import com.neueda.leap.Fill;
import com.neueda.leap.repository.FillRepository;
import com.neueda.leap.mapper.FillMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * MyBatis-based implementation of FillRepository
 */
@Repository
public class FillRepositoryImpl implements FillRepository {

    private final FillMapper fillMapper;

    public FillRepositoryImpl(FillMapper fillMapper) {
        this.fillMapper = fillMapper;
    }

    @Override
    public Fill findById(UUID fillId) {
        return fillMapper.selectFillById(fillId);
    }

    @Override
    public List<Fill> findByOrderId(UUID orderId) {
        return fillMapper.selectFillsByOrderId(orderId);
    }

    @Override
    public List<Fill> findByOrderIdPaginated(UUID orderId, int limit, int offset) {
        return fillMapper.selectFillsByOrderIdPaginated(orderId, limit, offset);
    }

    @Override
    public int countByOrderId(UUID orderId) {
        return fillMapper.countFillsByOrderId(orderId);
    }

    @Override
    public List<Fill> findByAccountId(UUID accountId, int limit, int offset) {
        return fillMapper.selectFillsByAccountId(accountId, limit, offset);
    }

    @Override
    public int countByAccountId(UUID accountId) {
        return fillMapper.countFillsByAccountId(accountId);
    }

    @Override
    public void save(UUID orderId, String price, String quantity, String status) {
        fillMapper.insertFill(orderId, price, quantity, status);
    }
}
