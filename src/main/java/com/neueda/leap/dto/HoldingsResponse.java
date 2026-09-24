package com.neueda.leap.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for holdings data (positions and cash)
 */
public class HoldingsResponse {
    private UUID accountId;
    private List<HoldingItem> holdings;
    private CashBalance cash;
    private OffsetDateTime asOfTimestamp;

    public HoldingsResponse() {
    }

    public HoldingsResponse(UUID accountId, List<HoldingItem> holdings, CashBalance cash, OffsetDateTime asOfTimestamp) {
        this.accountId = accountId;
        this.holdings = holdings;
        this.cash = cash;
        this.asOfTimestamp = asOfTimestamp;
    }

    // Getters and Setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public List<HoldingItem> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<HoldingItem> holdings) {
        this.holdings = holdings;
    }

    public CashBalance getCash() {
        return cash;
    }

    public void setCash(CashBalance cash) {
        this.cash = cash;
    }

    public OffsetDateTime getAsOfTimestamp() {
        return asOfTimestamp;
    }

    public void setAsOfTimestamp(OffsetDateTime asOfTimestamp) {
        this.asOfTimestamp = asOfTimestamp;
    }

    /**
     * Inner class for individual holdings
     */
    public static class HoldingItem {
        private UUID instrumentId;
        private String symbol;
        private String quantity;

        public HoldingItem() {
        }

        public HoldingItem(UUID instrumentId, String symbol, String quantity) {
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.quantity = quantity;
        }

        public UUID getInstrumentId() {
            return instrumentId;
        }

        public void setInstrumentId(UUID instrumentId) {
            this.instrumentId = instrumentId;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * Inner class for cash balance
     */
    public static class CashBalance {
        private String balance;
        private String currency;

        public CashBalance() {
        }

        public CashBalance(String balance, String currency) {
            this.balance = balance;
            this.currency = currency;
        }

        public String getBalance() {
            return balance;
        }

        public void setBalance(String balance) {
            this.balance = balance;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
