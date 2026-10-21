package com.neueda.leap.service;

import com.neueda.leap.Order;
import com.neueda.leap.enums.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Execution Pricing Tests")
class OrderExecutionServiceTest {

    private static final FauxnanceService.QuoteResponse QUOTE =
            new FauxnanceService.QuoteResponse("AAPL", 100.00, 99.90, 100.10, 1.0, 1.0, "2026-10-21T15:00:00Z");

    @Test
    @DisplayName("BUY executes at the ask and SELL at the bid")
    void pricesBuyAtAskAndSellAtBid() {
        assertEquals(new BigDecimal("100.10000"), OrderExecutionService.executionPrice(OrderSide.BUY, QUOTE));
        assertEquals(new BigDecimal("99.90000"), OrderExecutionService.executionPrice(OrderSide.SELL, QUOTE));
    }

    @Test
    @DisplayName("Falls back to the last price when the quote has no bid/ask")
    void fallsBackToLastPrice() {
        FauxnanceService.QuoteResponse noSpread =
                new FauxnanceService.QuoteResponse("AAPL", 100.00, 0, 0, 0, 0, "2026-10-21T15:00:00Z");
        assertEquals(new BigDecimal("100.00000"), OrderExecutionService.executionPrice(OrderSide.BUY, noSpread));
    }

    @Test
    @DisplayName("Market orders are never limit-rejected")
    void marketOrdersIgnoreLimit() {
        assertNull(OrderExecutionService.checkLimit(order(OrderSide.BUY, "MARKET", null), new BigDecimal("1000")));
    }

    @Test
    @DisplayName("A LIMIT BUY fills at or below its limit and is rejected above it")
    void limitBuy() {
        Order buy = order(OrderSide.BUY, "LIMIT", new BigDecimal("100.10"));
        assertNull(OrderExecutionService.checkLimit(buy, new BigDecimal("100.10")));
        assertNull(OrderExecutionService.checkLimit(buy, new BigDecimal("99.00")));
        String rejection = OrderExecutionService.checkLimit(buy, new BigDecimal("100.11"));
        assertNotNull(rejection);
        assertTrue(rejection.contains("above your limit of $100.10"));
    }

    @Test
    @DisplayName("A LIMIT SELL fills at or above its limit and is rejected below it")
    void limitSell() {
        Order sell = order(OrderSide.SELL, "LIMIT", new BigDecimal("99.90"));
        assertNull(OrderExecutionService.checkLimit(sell, new BigDecimal("99.90")));
        assertNull(OrderExecutionService.checkLimit(sell, new BigDecimal("101")));
        String rejection = OrderExecutionService.checkLimit(sell, new BigDecimal("99.89"));
        assertNotNull(rejection);
        assertTrue(rejection.contains("below your limit of $99.90"));
    }

    private static Order order(OrderSide side, String type, BigDecimal limit) {
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(), side, BigDecimal.ONE, "key");
        order.setOrderType(type);
        order.setLimitPrice(limit);
        return order;
    }
}
