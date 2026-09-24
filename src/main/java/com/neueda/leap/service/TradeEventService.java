package com.neueda.leap.service;

import com.neueda.leap.TradeEvent;
import com.neueda.leap.mapper.TradeEventMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for TradeEvent operations (audit trail)
 */
@Service
public class TradeEventService {

    private final TradeEventMapper tradeEventMapper;

    public TradeEventService(TradeEventMapper tradeEventMapper) {
        this.tradeEventMapper = tradeEventMapper;
    }

    /**
     * Get trade event by ID
     */
    public TradeEvent getTradeEventById(UUID tradeEventId) {
        return tradeEventMapper.selectTradeEventById(tradeEventId);
    }

    /**
     * List trade events for an entity (order/fill)
     */
    public List<TradeEvent> listTradeEventsByEntity(UUID entityId) {
        return tradeEventMapper.selectTradeEventsByEntity(entityId);
    }

    /**
     * List trade events for a client with pagination
     */
    public List<TradeEvent> listTradeEventsByClient(UUID clientId, int limit, int offset) {
        return tradeEventMapper.selectTradeEventsByClientId(clientId, limit, offset);
    }

    /**
     * Count trade events for a client
     */
    public int countTradeEventsByClient(UUID clientId) {
        return tradeEventMapper.countTradeEventsByClientId(clientId);
    }

    /**
     * Record a new trade event (immutable once created)
     */
    public void recordTradeEvent(UUID clientId, String entityType, UUID entityId, String action, String details) {
        validateTradeEvent(entityType, action);
        tradeEventMapper.insertTradeEvent(clientId, entityType, entityId, action, details);
    }

    /**
     * Record order submitted event
     */
    public void recordOrderSubmitted(UUID clientId, UUID orderId, String details) {
        recordTradeEvent(clientId, "ORDER", orderId, "SUBMITTED", details);
    }

    /**
     * Record order accepted event
     */
    public void recordOrderAccepted(UUID clientId, UUID orderId, String details) {
        recordTradeEvent(clientId, "ORDER", orderId, "ACCEPTED", details);
    }

    /**
     * Record fill executed event
     */
    public void recordFillExecuted(UUID clientId, UUID fillId, String details) {
        recordTradeEvent(clientId, "FILL", fillId, "FILL_EXECUTED", details);
    }

    /**
     * Validate trade event input
     */
    private void validateTradeEvent(String entityType, String action) {
        if (entityType == null || (!entityType.equals("ORDER") && !entityType.equals("FILL"))) {
            throw new IllegalArgumentException("Invalid entity type");
        }
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Action cannot be empty");
        }
    }
}
