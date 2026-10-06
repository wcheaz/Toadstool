package com.neueda.leap.service;

import com.neueda.leap.Fill;
import com.neueda.leap.repository.FillRepository;
import com.neueda.leap.validator.FillValidator;
import com.neueda.leap.validator.FillValidationRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Fill operations
 */
@Service
public class FillService {

    private final FillRepository fillRepository;
    private final FillValidator fillValidator;

    public FillService(FillRepository fillRepository, FillValidator fillValidator) {
        this.fillRepository = fillRepository;
        this.fillValidator = fillValidator;
    }

    /**
     * Get fill by ID
     */
    public Fill getFillById(UUID fillId) {
        return fillRepository.findById(fillId);
    }

    /**
     * List fills for an order
     */
    public List<Fill> listFillsByOrder(UUID orderId) {
        return fillRepository.findByOrderId(orderId);
    }

    /**
     * List fills for an order with pagination
     */
    public List<Fill> listFillsByOrderPaginated(UUID orderId, int limit, int offset) {
        return fillRepository.findByOrderIdPaginated(orderId, limit, offset);
    }

    /**
     * Count fills for an order
     */
    public int countFillsByOrder(UUID orderId) {
        return fillRepository.countByOrderId(orderId);
    }

    /**
     * List fills for an account with pagination
     */
    public List<Fill> listFillsByAccount(UUID accountId, int limit, int offset) {
        return fillRepository.findByAccountId(accountId, limit, offset);
    }

    /**
     * Count fills for an account
     */
    public int countFillsByAccount(UUID accountId) {
        return fillRepository.countByAccountId(accountId);
    }

    /**
     * Create a new fill (used by order execution engine)
     */
    public void createFill(UUID orderId, String price, String quantity, String status) {
        fillValidator.validate(new FillValidationRequest(price, quantity, status));
        fillRepository.save(orderId, price, quantity, status);
    }
}
