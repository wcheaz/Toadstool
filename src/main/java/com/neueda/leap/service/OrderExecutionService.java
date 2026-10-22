package com.neueda.leap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.leap.Account;
import com.neueda.leap.Fill;
import com.neueda.leap.Instrument;
import com.neueda.leap.Order;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.mapper.AccountMapper;
import com.neueda.leap.repository.OrderRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes accepted orders on the server, separately from their acceptance (BR-06).
 *
 * Each order is priced against the live market quote at the point of execution (BR-08): a BUY at the ask,
 * a SELL at the bid, and a LIMIT order only if that price is at or better than its limit. The pricing
 * decision, the fill and any rejection are written to the append-only trade record (BR-14/BR-15), and
 * every order ends with exactly one outcome, FILLED or REJECTED (ux_fills_order).
 *
 * Each execution locks the account row, so even with several backend instances two orders for the same
 * account can never spend the same cash or sell the same units; within an instance orders also run one at a
 * time on a single worker thread. Execution is idempotent: an order that already has an outcome is skipped,
 * and the one-outcome-per-order index (ux_fills_order) rejects a second fill from another instance. A sweeper picks up orders whose execution was missed (e.g. after a restart) and
 * rejects any that waited longer than the execution window, rather than filling them at a stale time.
 */
@Service
public class OrderExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderExecutionService.class);

    /** An order not executed within this window is rejected instead of being filled late. */
    static final Duration EXECUTION_WINDOW = Duration.ofMinutes(2);
    /** The sweeper leaves very recent orders to the immediate execution started at acceptance. */
    private static final Duration SWEEP_GRACE = Duration.ofSeconds(3);
    private static final int SWEEP_BATCH_SIZE = 50;
    private static final String STATUS_ACCEPTED = "ACCEPTED";

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final InstrumentService instrumentService;
    private final HoldingsService holdingsService;
    private final FauxnanceService fauxnanceService;
    private final FillService fillService;
    private final TradeEventService tradeEventService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "order-execution");
        thread.setDaemon(true);
        return thread;
    });

    public OrderExecutionService(OrderService orderService,
                                 OrderRepository orderRepository,
                                 AccountService accountService,
                                 AccountMapper accountMapper,
                                 InstrumentService instrumentService,
                                 HoldingsService holdingsService,
                                 FauxnanceService fauxnanceService,
                                 FillService fillService,
                                 TradeEventService tradeEventService,
                                 PlatformTransactionManager transactionManager,
                                 ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.instrumentService = instrumentService;
        this.holdingsService = holdingsService;
        this.fauxnanceService = fauxnanceService;
        this.fillService = fillService;
        this.tradeEventService = tradeEventService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    void shutdown() {
        worker.shutdown();
    }

    /** Queue an accepted order for execution; the client follows its status (BR-07). */
    public void executeAsync(UUID orderId) {
        worker.execute(() -> executeSafely(orderId));
    }

    /** Execute an order now, waiting for its outcome. Safe to call for an order that already has one. */
    public void executeNow(UUID orderId) {
        try {
            worker.submit(() -> executeSafely(orderId)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.ExecutionException e) {
            LOGGER.error("Execution of order {} failed", orderId, e.getCause());
        }
    }

    /** Recovery: execute orders whose immediate execution never ran or did not finish. */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void executePendingOrders() {
        try {
            List<Order> pending = orderRepository.findUnexecuted(OffsetDateTime.now().minus(SWEEP_GRACE), SWEEP_BATCH_SIZE);
            pending.forEach(order -> worker.execute(() -> executeSafely(order.getOrderId())));
        } catch (Exception e) {
            LOGGER.error("Could not look up orders awaiting execution", e);
        }
    }

    private void executeSafely(UUID orderId) {
        try {
            execute(orderId);
        } catch (Exception e) {
            // Left ACCEPTED: the sweeper retries until the execution window passes, then rejects it
            LOGGER.error("Execution of order {} failed; it will be retried", orderId, e);
        }
    }

    void execute(UUID orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null || !STATUS_ACCEPTED.equals(order.getStatus())) {
            return; // unknown, or already has an outcome
        }
        Account account = accountService.getAccountById(order.getAccountId());
        UUID clientId = account != null ? account.getClientId() : null;

        if (order.getSubmittedAt() != null
                && order.getSubmittedAt().plus(EXECUTION_WINDOW).isBefore(OffsetDateTime.now())) {
            reject(order, clientId, null, null, "The order expired before it could be executed.");
            return;
        }

        Instrument instrument = instrumentService.getInstrumentById(order.getInstrumentId());
        FauxnanceService.QuoteResponse quote = instrument != null ? fetchQuote(instrument.getSymbol()) : null;
        if (quote == null || !(quote.getPrice() > 0)) {
            reject(order, clientId, null, quote, "No current market price was available, so the order was not executed.");
            return;
        }

        BigDecimal executionPrice = executionPrice(order.getSide(), quote);
        String limitRejection = checkLimit(order, executionPrice);
        if (limitRejection != null) {
            reject(order, clientId, executionPrice, quote, limitRejection);
            return;
        }

        // The pricing decision is permanent even if execution then fails (BR-14)
        tradeEventService.recordOrderPriced(clientId, orderId, toJson(pricingDetails(order, executionPrice, quote)));

        try {
            transactionTemplate.executeWithoutResult(status -> {
                accountMapper.lockForUpdate(order.getAccountId());
                if (OrderSide.SELL.equals(order.getSide())) {
                    BigDecimal held = holdingsService.getHolding(order.getAccountId(), order.getInstrumentId())
                            .map(holding -> holding.getQuantity()).orElse(BigDecimal.ZERO);
                    if (held.compareTo(order.getQuantity()) < 0) {
                        throw new IllegalArgumentException("You no longer hold enough units to sell.");
                    }
                }
                // Cash, holdings and the fill change together or not at all (BR-09)
                Fill fill = orderService.fillOrder(orderId, executionPrice);
                Map<String, Object> details = pricingDetails(order, executionPrice, quote);
                details.put("orderId", orderId.toString());
                details.put("quantity", order.getQuantity().toPlainString());
                details.put("fee", fill.getFee() != null ? fill.getFee().toPlainString() : "0");
                tradeEventService.recordFillExecuted(clientId, fill.getFillId(), toJson(details));
            });
        } catch (DuplicateKeyException e) {
            LOGGER.info("Order {} already has an execution outcome", orderId);
        } catch (IllegalArgumentException e) {
            reject(order, clientId, executionPrice, quote, friendlyReason(e.getMessage()));
        }
    }

    /** BUY at the ask, SELL at the bid; the last price when the quote has no usable bid/ask. */
    static BigDecimal executionPrice(OrderSide side, FauxnanceService.QuoteResponse quote) {
        double sidePrice = OrderSide.BUY.equals(side) ? quote.getAsk() : quote.getBid();
        double price = sidePrice > 0 ? sidePrice : quote.getPrice();
        return BigDecimal.valueOf(price).setScale(5, RoundingMode.HALF_UP);
    }

    /** A LIMIT BUY fills at or below its limit; a LIMIT SELL at or above it. Null when acceptable. */
    static String checkLimit(Order order, BigDecimal executionPrice) {
        if (!OrderService.ORDER_TYPE_LIMIT.equals(order.getOrderType()) || order.getLimitPrice() == null) {
            return null;
        }
        BigDecimal limit = order.getLimitPrice();
        if (OrderSide.BUY.equals(order.getSide()) && executionPrice.compareTo(limit) > 0) {
            return "The market price (" + money(executionPrice) + ") was above your limit of " + money(limit) + ".";
        }
        if (OrderSide.SELL.equals(order.getSide()) && executionPrice.compareTo(limit) < 0) {
            return "The market price (" + money(executionPrice) + ") was below your limit of " + money(limit) + ".";
        }
        return null;
    }

    private void reject(Order order, UUID clientId, BigDecimal price, FauxnanceService.QuoteResponse quote, String reason) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                fillService.createRejectedFill(order.getOrderId(), price, order.getQuantity(), reason);
                Map<String, Object> details = quote != null && price != null
                        ? pricingDetails(order, price, quote)
                        : new LinkedHashMap<>();
                details.put("reason", reason);
                tradeEventService.recordOrderRejected(clientId, order.getOrderId(), toJson(details));
            });
            LOGGER.info("Order {} rejected: {}", order.getOrderId(), reason);
        } catch (DuplicateKeyException e) {
            LOGGER.info("Order {} already has an execution outcome", order.getOrderId());
        }
    }

    private FauxnanceService.QuoteResponse fetchQuote(String symbol) {
        try {
            return fauxnanceService.getQuote(symbol);
        } catch (Exception e) {
            LOGGER.warn("Quote lookup failed for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> pricingDetails(Order order, BigDecimal executionPrice, FauxnanceService.QuoteResponse quote) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("side", order.getSide() != null ? order.getSide().name() : null);
        details.put("orderType", order.getOrderType());
        if (order.getLimitPrice() != null) {
            details.put("limitPrice", order.getLimitPrice().toPlainString());
        }
        details.put("executionPrice", executionPrice.toPlainString());
        Map<String, Object> quoteSnapshot = new LinkedHashMap<>();
        quoteSnapshot.put("symbol", quote.getSymbol());
        quoteSnapshot.put("price", quote.getPrice());
        quoteSnapshot.put("bid", quote.getBid());
        quoteSnapshot.put("ask", quote.getAsk());
        quoteSnapshot.put("asOf", quote.getAsOf());
        quoteSnapshot.put("currency", quote.getCurrency());
        if (quote.getLocalCurrency() != null) {
            // Priced in another currency and converted to USD at this rate
            quoteSnapshot.put("localCurrency", quote.getLocalCurrency());
            quoteSnapshot.put("localPrice", quote.getLocalPrice());
            quoteSnapshot.put("usdPerUnit", quote.getFxRate());
        }
        quoteSnapshot.put("source", "Fauxnance");
        details.put("quote", quoteSnapshot);
        return details;
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /** Turns internal validation messages into wording a client can act on. */
    private static String friendlyReason(String message) {
        if (message == null) {
            return "The order could not be executed.";
        }
        if (message.startsWith("Insufficient balance")) {
            return "Not enough cash at the execution price.";
        }
        return message;
    }

    private static String money(BigDecimal value) {
        return "$" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
