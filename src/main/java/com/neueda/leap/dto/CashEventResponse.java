package com.neueda.leap.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A deposit, withdrawal, or trade settlement posted against an account's cash balance.
 */
public class CashEventResponse {

    private UUID cashEventId;
    private UUID accountId;
    private UUID orderId;
    private String eventType;
    private BigDecimal amount;
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    private OffsetDateTime occurredAt;
    private String details;

    public UUID getCashEventId() { return cashEventId; }
    public void setCashEventId(UUID cashEventId) { this.cashEventId = cashEventId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
