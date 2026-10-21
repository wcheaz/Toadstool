package com.neueda.leap.controller;

import com.neueda.leap.Order;
import com.neueda.leap.dto.OrderResponse;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.security.SecurityAccess;
import com.neueda.leap.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoints for platform-wide order reporting.
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /api/admin/orders
     * List all orders, optionally restricted by submission time.
     * The from bound is inclusive and the to bound is exclusive.
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<OrderResponse>> listAllOrders(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (!SecurityAccess.hasRole(SecurityAccess.ADMIN_ROLE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (limit < 1 || limit > 1000 || offset < 0 || (from != null && to != null && !from.isBefore(to))) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<OrderResponse> orders = orderService.listAllOrders(from, to, limit, offset).stream()
                    .map(this::mapToOrderResponse)
                    .collect(Collectors.toList());
            int totalCount = orderService.countAllOrders(from, to);
            return ResponseEntity.ok(new PaginatedResponse<>(orders, limit, offset, totalCount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        String side = order.getSide() != null ? order.getSide().toString() : "BUY";
        return new OrderResponse(
                order.getOrderId(),
                order.getAccountId(),
                order.getInstrumentId(),
                side,
                order.getQuantity().toPlainString(),
                order.getIdempotencyKey(),
                order.getStatus() != null ? order.getStatus() : "ACCEPTED",
                order.getSubmittedAt());
    }
}