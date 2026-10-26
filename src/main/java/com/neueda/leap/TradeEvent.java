package com.neueda.leap;

import java.time.OffsetDateTime;
import java.util.UUID;
import com.neueda.leap.enums.TradeEventEntityType;

public class TradeEvent {

    private UUID tradeEventId;
    private UUID clientId;
    private TradeEventEntityType entityType;
    private UUID entityId;
    private String action;
    private OffsetDateTime occurredAt;
    private String details;

    // Constructors
    public TradeEvent() {
    }

    public TradeEvent(TradeEventEntityType entityType, UUID entityId, String action) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
    }

    public TradeEvent(UUID clientId, TradeEventEntityType entityType, UUID entityId, String action, String details) {
        this.clientId = clientId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.details = details;
    }

    public TradeEvent(UUID tradeEventId, UUID clientId, TradeEventEntityType entityType, UUID entityId, String action, OffsetDateTime occurredAt, String details) {
        this.tradeEventId = tradeEventId;
        this.clientId = clientId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.occurredAt = occurredAt;
        this.details = details;
    }

    // Getters and Setters
    public UUID getTradeEventId() {
        return tradeEventId;
    }

    public void setTradeEventId(UUID tradeEventId) {
        this.tradeEventId = tradeEventId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public TradeEventEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(TradeEventEntityType entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
