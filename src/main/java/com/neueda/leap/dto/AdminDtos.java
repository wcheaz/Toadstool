package com.neueda.leap.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read models for the admin oversight API. Fields are public so MyBatis can map
 * query rows straight into them and Jackson can serialise them without boilerplate.
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    /** One row of the client oversight search results. */
    public static class ClientSummary {
        public UUID clientId;
        public String displayName;
        public String email;
        public String username;
        public String status;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime createdAt;
        public int accountCount;
        public BigDecimal totalCash;
        public UUID primaryAccountId;
        public String primaryAccountName;
        public String latestActivity;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime latestActivityAt;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime lastLoginAt;
    }

    public static class ClientAccount {
        public UUID accountId;
        public String accountName;
        public String status;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime openedAt;
        public BigDecimal balance;
    }

    /** Client header plus every account the client owns. */
    public static class ClientDetail {
        public ClientSummary client;
        public List<ClientAccount> accounts;
    }

    public static class AccessEvent {
        public UUID accessEventId;
        public UUID adminUserId;
        public String adminEmail;
        public UUID clientId;
        public String clientName;
        public String action;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime occurredAt;
        public String details;
    }

    public static class TradeEventRow {
        public UUID tradeEventId;
        public UUID clientId;
        public String clientName;
        public String entityType;
        public UUID entityId;
        public String action;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime occurredAt;
        public String details;
    }

    public static class LoginEventRow {
        public UUID loginEventId;
        public UUID clientId;
        public String clientName;
        public String emailAttempted;
        public String outcome;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime occurredAt;
        public String details;
    }

    public static class PlatformActivityRow {
        @JsonSerialize(using = LocalDateSerializer.class)
        public LocalDate activityDate;
        public int orderCount;
        public BigDecimal filledQuantity;
        public BigDecimal grossAmount;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime updatedAt;
    }

    public static class InstrumentActivityRow {
        @JsonSerialize(using = LocalDateSerializer.class)
        public LocalDate activityDate;
        public UUID instrumentId;
        public String symbol;
        public int orderCount;
        public BigDecimal filledQuantity;
        public BigDecimal grossAmount;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        public OffsetDateTime updatedAt;
    }

    /** Headline platform numbers for the admin dashboard. */
    public static class PlatformOverview {
        public int totalClients;
        public int activeClients;
        public int totalAccounts;
        public BigDecimal totalCash;
        public int totalOrders;
        public int filledOrders;
        public int openOrders;
        public int ordersLast30Days;
        public BigDecimal grossVolumeLast30Days;
        public BigDecimal feesLast30Days;
        public int failedLoginsLast24Hours;
    }

    public static class TopInstrument {
        public UUID instrumentId;
        public String symbol;
        public String name;
        public int fillCount;
        public BigDecimal grossAmount;
    }
}
