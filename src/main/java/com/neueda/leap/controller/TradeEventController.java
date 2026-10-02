package com.neueda.leap.controller;

import com.neueda.leap.TradeEvent;
import com.neueda.leap.Account;
import com.neueda.leap.Order;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.security.SecurityAccess;
import com.neueda.leap.service.TradeEventService;
import com.neueda.leap.service.OrderService;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for TradeEvent endpoints (audit trail)
 */
@RestController
@RequestMapping("/api")
public class TradeEventController {

    private final TradeEventService tradeEventService;
    private final OrderService orderService;
    private final AccountService accountService;
    private final ClientService clientService;

    public TradeEventController(TradeEventService tradeEventService, OrderService orderService,
                                AccountService accountService, ClientService clientService) {
        this.tradeEventService = tradeEventService;
        this.orderService = orderService;
        this.accountService = accountService;
        this.clientService = clientService;
    }

    /**
     * GET /api/orders/{orderId}/events
     * Get full lifecycle history of an order (audit trail)
     */
    @GetMapping("/orders/{orderId}/events")
    public ResponseEntity<TradeEventResponse> listTradeEventsByOrder(
            @PathVariable UUID orderId) {

        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            Account account = accountService.getAccountById(order.getAccountId());
            if (account == null) {
                return ResponseEntity.notFound().build();
            }
            if (!SecurityAccess.canAccessAccount(order.getAccountId(), account.getClientId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<TradeEvent> events = tradeEventService.listTradeEventsByEntity(orderId);
            List<TradeEventDto> eventDtos = events.stream()
                    .map(this::mapToTradeEventDto)
                    .collect(Collectors.toList());

            TradeEventResponse response = new TradeEventResponse(eventDtos, eventDtos.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/clients/{clientId}/events
     * Get all order/fill events for a client
     */
    @GetMapping("/clients/{clientId}/events")
    public ResponseEntity<PaginatedResponse<TradeEventDto>> listTradeEventsByClient(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (!SecurityAccess.canAccessClient(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Verify client exists
            if (clientService.getClientById(clientId) == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<TradeEvent> events = tradeEventService.listTradeEventsByClient(clientId, limit, offset);
            int totalCount = tradeEventService.countTradeEventsByClient(clientId);

            List<TradeEventDto> eventDtos = events.stream()
                    .map(this::mapToTradeEventDto)
                    .collect(Collectors.toList());

            PaginatedResponse<TradeEventDto> pagedResponse = new PaginatedResponse<>(eventDtos, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map TradeEvent domain object to TradeEventDto
     */
    private TradeEventDto mapToTradeEventDto(TradeEvent event) {
        String entityTypeStr = event.getEntityType() != null ? event.getEntityType().toString() : "ORDER";
        
        return new TradeEventDto(
                event.getTradeEventId(),
                event.getClientId(),
                entityTypeStr,
                event.getEntityId(),
                event.getAction(),
                event.getOccurredAt(),
                event.getDetails()
        );
    }

    /**
     * DTO for TradeEvent response
     */
    public static class TradeEventDto {
        private UUID tradeEventId;
        private UUID clientId;
        private String entityType;
        private UUID entityId;
        private String action;
        private OffsetDateTime occurredAt;
        private String details;

        public TradeEventDto(UUID tradeEventId, UUID clientId, String entityType, UUID entityId,
                            String action, OffsetDateTime occurredAt, String details) {
            this.tradeEventId = tradeEventId;
            this.clientId = clientId;
            this.entityType = entityType;
            this.entityId = entityId;
            this.action = action;
            this.occurredAt = occurredAt;
            this.details = details;
        }

        // Getters
        public UUID getTradeEventId() { return tradeEventId; }
        public UUID getClientId() { return clientId; }
        public String getEntityType() { return entityType; }
        public UUID getEntityId() { return entityId; }
        public String getAction() { return action; }
        public OffsetDateTime getOccurredAt() { return occurredAt; }
        public String getDetails() { return details; }
    }

    /**
     * Response wrapper for trade events (no pagination for order events)
     */
    public static class TradeEventResponse {
        private List<TradeEventDto> events;
        private int totalCount;

        public TradeEventResponse(List<TradeEventDto> events, int totalCount) {
            this.events = events;
            this.totalCount = totalCount;
        }

        public List<TradeEventDto> getEvents() { return events; }
        public int getTotalCount() { return totalCount; }
    }
}
