package com.neueda.leap.controller;

import com.neueda.leap.Order;
import com.neueda.leap.service.OrderService;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.InstrumentService;
import com.neueda.leap.dto.PlaceOrderRequest;
import com.neueda.leap.dto.OrderResponse;
import com.neueda.leap.dto.PaginatedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Order endpoints
 * Handles order placement, retrieval, and listing
 */
@RestController
@RequestMapping("/api/accounts/{accountId}/orders")
public class OrderController {

    private final OrderService orderService;
    private final AccountService accountService;
    private final InstrumentService instrumentService;

    public OrderController(OrderService orderService, AccountService accountService, InstrumentService instrumentService) {
        this.orderService = orderService;
        this.accountService = accountService;
        this.instrumentService = instrumentService;
    }

    /**
     * POST /api/accounts/{accountId}/orders
     * Place a new buy/sell order
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @PathVariable UUID accountId,
            @RequestBody PlaceOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        try {
            // Validate account exists
            if (accountService.getAccountById(accountId) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Validate instrument exists and is tradable
            if (instrumentService.getInstrumentById(request.getInstrumentId()) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Generate idempotency key if not provided
            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                idempotencyKey = "order-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            }

            // Check for duplicate idempotency key
            Order existing = orderService.getOrderByIdempotencyKey(accountId, idempotencyKey);
            if (existing != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(mapToOrderResponse(existing));
            }

            // Create order
            Order order = orderService.createOrder(accountId, request.getInstrumentId(), 
                    request.getSide(), request.getQuantity(), idempotencyKey);

            return ResponseEntity.status(HttpStatus.CREATED).body(mapToOrderResponse(order));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/orders/{orderId}
     * Retrieve a single order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId) {

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify order belongs to account
        if (!order.getAccountId().equals(accountId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(mapToOrderResponse(order));
    }

    /**
     * GET /api/accounts/{accountId}/orders
     * List orders for an account with pagination
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<OrderResponse>> listOrders(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Validate account exists
            if (accountService.getAccountById(accountId) == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate pagination params
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<Order> orders = orderService.listOrdersByAccount(accountId, limit, offset);
            int totalCount = orderService.countOrdersByAccount(accountId);

            List<OrderResponse> responses = orders.stream()
                    .map(this::mapToOrderResponse)
                    .collect(Collectors.toList());

            PaginatedResponse<OrderResponse> pagedResponse = new PaginatedResponse<>(responses, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/accounts/{accountId}/quote-preview
     * Get indicative price for a hypothetical order
     * (Stub implementation - returns fixed price for now)
     */
    @GetMapping("/quote-preview")
    public ResponseEntity<QuotePreviewResponse> getQuotePreview(
            @PathVariable UUID accountId,
            @RequestParam UUID instrumentId,
            @RequestParam String side,
            @RequestParam String quantity) {

        try {
            // Validate account exists
            if (accountService.getAccountById(accountId) == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate instrument exists
            var instrument = instrumentService.getInstrumentById(instrumentId);
            if (instrument == null) {
                return ResponseEntity.notFound().build();
            }

            // Stub: return fixed indicative price
            String indicativePrice = "100.0000000000";
            java.math.BigDecimal qty = new java.math.BigDecimal(quantity);
            java.math.BigDecimal price = new java.math.BigDecimal(indicativePrice);
            String estimatedTotal = qty.multiply(price).toPlainString();

            QuotePreviewResponse response = new QuotePreviewResponse(
                    instrumentId,
                    instrument.getSymbol(),
                    side,
                    quantity,
                    indicativePrice,
                    estimatedTotal,
                    OffsetDateTime.now(),
                    "Indicative price only; actual execution price may differ"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map Order domain object to OrderResponse DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        String sideStr = order.getSide() != null ? order.getSide().toString() : "BUY";
        
        return new OrderResponse(
                order.getOrderId(),
                order.getAccountId(),
                order.getInstrumentId(),
                sideStr,
                order.getQuantity().toPlainString(),
                order.getIdempotencyKey(),
                "SUBMITTED",  // Default status for newly created orders
                order.getSubmittedAt()
        );
    }

    /**
     * DTO for quote preview response
     */
    public static class QuotePreviewResponse {
        private UUID instrumentId;
        private String symbol;
        private String side;
        private String quantity;
        private String indicativePrice;
        private String estimatedTotal;
        private OffsetDateTime timestamp;
        private String notes;

        public QuotePreviewResponse(UUID instrumentId, String symbol, String side, String quantity,
                                     String indicativePrice, String estimatedTotal,
                                     OffsetDateTime timestamp, String notes) {
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.side = side;
            this.quantity = quantity;
            this.indicativePrice = indicativePrice;
            this.estimatedTotal = estimatedTotal;
            this.timestamp = timestamp;
            this.notes = notes;
        }

        // Getters
        public UUID getInstrumentId() { return instrumentId; }
        public String getSymbol() { return symbol; }
        public String getSide() { return side; }
        public String getQuantity() { return quantity; }
        public String getIndicativePrice() { return indicativePrice; }
        public String getEstimatedTotal() { return estimatedTotal; }
        public OffsetDateTime getTimestamp() { return timestamp; }
        public String getNotes() { return notes; }
    }
}
