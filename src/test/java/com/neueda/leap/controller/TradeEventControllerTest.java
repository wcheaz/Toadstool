package com.neueda.leap.controller;

import com.neueda.leap.Client;
import com.neueda.leap.Order;
import com.neueda.leap.TradeEvent;
import com.neueda.leap.enums.ClientStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.enums.TradeEventEntityType;
import com.neueda.leap.service.ClientService;
import com.neueda.leap.service.OrderService;
import com.neueda.leap.service.TradeEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Trade Event Controller Tests")
class TradeEventControllerTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-22T14:30:00Z");

    @Mock
    private TradeEventService tradeEventService;

    @Mock
    private OrderService orderService;

    @Mock
    private ClientService clientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new TradeEventController(tradeEventService, orderService, clientService));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/events returns the non-paginated event history contract")
    void listTradeEventsByOrderReturnsEventHistory() throws Exception {
        UUID clientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        TradeEvent event = new TradeEvent(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                clientId,
                TradeEventEntityType.ORDER,
                orderId,
                "SUBMITTED",
                OCCURRED_AT,
                "{\"status\":\"queued\"}"
        );

        when(orderService.getOrderById(orderId))
                .thenReturn(new Order(orderId, UUID.fromString("44444444-4444-4444-4444-444444444444"), UUID.fromString("55555555-5555-5555-5555-555555555555"), OrderSide.BUY, new BigDecimal("2.0000000000"), "idem", OCCURRED_AT));
        when(tradeEventService.listTradeEventsByEntity(orderId)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/orders/{orderId}/events", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].tradeEventId").value(event.getTradeEventId().toString()))
                .andExpect(jsonPath("$.events[0].clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.events[0].entityType").value("ORDER"))
                .andExpect(jsonPath("$.events[0].entityId").value(orderId.toString()))
                .andExpect(jsonPath("$.events[0].action").value("SUBMITTED"))
                .andExpect(jsonPath("$.events[0].occurredAt").value(ControllerTestSupport.isoTimestamp(OCCURRED_AT)))
                .andExpect(jsonPath("$.events[0].details").value("{\"status\":\"queued\"}"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("GET /api/clients/{clientId}/events returns paginated client trade events")
    void listTradeEventsByClientReturnsPaginatedItems() throws Exception {
        UUID clientId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        UUID entityId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        TradeEvent event = new TradeEvent(
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                clientId,
                TradeEventEntityType.FILL,
                entityId,
                "FILL_EXECUTED",
                OCCURRED_AT,
                "{\"price\":\"101.00\"}"
        );

        when(clientService.getClientById(clientId))
                .thenReturn(new Client(clientId, "client@example.com", "Alice", ClientStatus.ACTIVE, OCCURRED_AT, OCCURRED_AT));
        when(tradeEventService.listTradeEventsByClient(clientId, 2, 0)).thenReturn(List.of(event));
        when(tradeEventService.countTradeEventsByClient(clientId)).thenReturn(1);

        mockMvc.perform(get("/api/clients/{clientId}/events", clientId)
                        .param("entityType", "FILL")
                        .param("limit", "2")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tradeEventId").value(event.getTradeEventId().toString()))
                .andExpect(jsonPath("$.items[0].entityType").value("FILL"))
                .andExpect(jsonPath("$.items[0].action").value("FILL_EXECUTED"))
                .andExpect(jsonPath("$.pagination.totalCount").value(1));
    }
}
