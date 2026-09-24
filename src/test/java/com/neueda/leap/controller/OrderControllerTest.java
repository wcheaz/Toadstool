package com.neueda.leap.controller;

import com.neueda.leap.Account;
import com.neueda.leap.Instrument;
import com.neueda.leap.Order;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.InstrumentService;
import com.neueda.leap.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Controller Tests")
class OrderControllerTest {

    private static final OffsetDateTime SUBMITTED_AT = OffsetDateTime.parse("2026-09-22T14:30:00Z");

    @Mock
    private OrderService orderService;

    @Mock
    private AccountService accountService;

    @Mock
    private InstrumentService instrumentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new OrderController(orderService, accountService, instrumentService));
    }

    @Test
    @DisplayName("POST /api/accounts/{accountId}/orders creates an order for the YAML contract")
    void placeOrderReturnsCreatedOrder() throws Exception {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID clientId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID instrumentId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Order order = new Order(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                accountId,
                instrumentId,
                OrderSide.BUY,
                new BigDecimal("2.5000000000"),
                "idem-123",
                SUBMITTED_AT
        );

        when(accountService.getAccountById(accountId)).thenReturn(new Account(accountId, clientId, AccountStatus.ACTIVE, SUBMITTED_AT));
        when(instrumentService.getInstrumentById(instrumentId))
                .thenReturn(new Instrument(instrumentId, "AAPL", "Apple Inc.", AssetClass.EQUITY, InstrumentStatus.TRADABLE));
        when(orderService.getOrderByIdempotencyKey(accountId, "idem-123")).thenReturn(null);
        when(orderService.createOrder(accountId, instrumentId, "BUY", "2.5000000000", "idem-123")).thenReturn(order);

        mockMvc.perform(post("/api/accounts/{accountId}/orders", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idem-123")
                        .content("{\"instrumentId\":\"" + instrumentId + "\",\"side\":\"BUY\",\"quantity\":\"2.5000000000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(order.getOrderId().toString()))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.instrumentId").value(instrumentId.toString()))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.quantity").value("2.5000000000"))
                .andExpect(jsonPath("$.idempotencyKey").value("idem-123"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").value(ControllerTestSupport.isoTimestamp(SUBMITTED_AT)));

        verify(orderService).createOrder(accountId, instrumentId, "BUY", "2.5000000000", "idem-123");
    }

    @Test
    @DisplayName("POST /api/accounts/{accountId}/orders returns 409 for duplicate idempotency keys")
    void placeOrderReturnsConflictForDuplicateIdempotencyKey() throws Exception {
        UUID accountId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID clientId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID instrumentId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Order existing = new Order(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                accountId,
                instrumentId,
                OrderSide.SELL,
                new BigDecimal("1.0000000000"),
                "dup-key",
                SUBMITTED_AT
        );

        when(accountService.getAccountById(accountId)).thenReturn(new Account(accountId, clientId, AccountStatus.ACTIVE, SUBMITTED_AT));
        when(instrumentService.getInstrumentById(instrumentId))
                .thenReturn(new Instrument(instrumentId, "MSFT", "Microsoft", AssetClass.EQUITY, InstrumentStatus.TRADABLE));
        when(orderService.getOrderByIdempotencyKey(accountId, "dup-key")).thenReturn(existing);

        mockMvc.perform(post("/api/accounts/{accountId}/orders", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "dup-key")
                        .content("{\"instrumentId\":\"" + instrumentId + "\",\"side\":\"SELL\",\"quantity\":\"1.0000000000\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.orderId").value(existing.getOrderId().toString()))
                .andExpect(jsonPath("$.idempotencyKey").value("dup-key"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} uses the YAML lookup path")
    void getOrderUsesYamlPath() throws Exception {
        UUID accountId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID orderId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID instrumentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Order order = new Order(orderId, accountId, instrumentId, OrderSide.BUY, new BigDecimal("3.0000000000"), "idem-999", SUBMITTED_AT);

        when(orderService.getOrderById(orderId)).thenReturn(order);

        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.instrumentId").value(instrumentId.toString()));

        verify(orderService).getOrderById(orderId);
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId}/orders returns a paginated order list")
    void listOrdersReturnsPaginatedItems() throws Exception {
        UUID accountId = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
        UUID clientId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID instrumentId = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
        Order order = new Order(
                UUID.fromString("cccccccc-1111-1111-1111-111111111111"),
                accountId,
                instrumentId,
                OrderSide.SELL,
                new BigDecimal("4.0000000000"),
                "idem-456",
                SUBMITTED_AT
        );

        when(accountService.getAccountById(accountId)).thenReturn(new Account(accountId, clientId, AccountStatus.ACTIVE, SUBMITTED_AT));
        when(orderService.listOrdersByAccount(accountId, 2, 1)).thenReturn(List.of(order));
        when(orderService.countOrdersByAccount(accountId)).thenReturn(3);

        mockMvc.perform(get("/api/accounts/{accountId}/orders", accountId)
                        .param("limit", "2")
                        .param("offset", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderId").value(order.getOrderId().toString()))
                .andExpect(jsonPath("$.items[0].side").value("SELL"))
                .andExpect(jsonPath("$.pagination.limit").value(2))
                .andExpect(jsonPath("$.pagination.offset").value(1))
                .andExpect(jsonPath("$.pagination.totalCount").value(3))
                .andExpect(jsonPath("$.pagination.hasMore").value(false));
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId}/quote-preview returns the quote preview schema")
    void getQuotePreviewReturnsYamlFields() throws Exception {
        UUID accountId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID clientId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID instrumentId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        when(accountService.getAccountById(accountId)).thenReturn(new Account(accountId, clientId, AccountStatus.ACTIVE, SUBMITTED_AT));
        when(instrumentService.getInstrumentById(instrumentId))
                .thenReturn(new Instrument(instrumentId, "AAPL", "Apple Inc.", AssetClass.EQUITY, InstrumentStatus.TRADABLE));

        mockMvc.perform(get("/api/accounts/{accountId}/quote-preview", accountId)
                        .param("instrumentId", instrumentId.toString())
                        .param("side", "BUY")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrumentId").value(instrumentId.toString()))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.quantity").value("2"))
                .andExpect(jsonPath("$.indicativePrice").value("100.0000000000"))
                .andExpect(jsonPath("$.estimatedTotal").value("200.0000000000"))
                .andExpect(jsonPath("$.notes").value("Indicative price only; actual execution price may differ"));
    }
}
